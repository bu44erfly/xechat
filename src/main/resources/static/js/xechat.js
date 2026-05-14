document.write('<script src="js/config.js"></script>');

// 头像数量
var head_num = 50;
// 用户id
var uid = null;
var stompClient = null;
var onlineUserList;
var userMap = {};
var onlineUserMap = {};
var CHANNEL_PEER_PREFIX = 'channel:';
var joinedChannelIds = {};
var channelSubscriptions = {};
var channelUnreadCount = {};
var activeChannelId = 'lobby';
var activeChatUserId = channelPeerId(activeChannelId);
var chatHistory = {};
var unreadCount = {};
var address = '未知地区';

var friendMap = {};

var title = document.title;
// 是否打开通知
var openNotice = true;
// 通知权限，0不支持通知 1允许通知 2不允许通知 3未获取权限
var permission = 3;
// 最新的消息数量
var newMsgTotal = 0;
// 窗口可见
var visible = true;
// 是否打开提示音
var opendSound = true;

var ADMIN_TOKEN_KEY = 'xechat_admin_token';

function channelPeerId(channelId) {
    return CHANNEL_PEER_PREFIX + ('' + channelId).trim();
}

function isChannelPeerId(peerId) {
    return peerId != null && ('' + peerId).indexOf(CHANNEL_PEER_PREFIX) === 0;
}

function peerIdToChannelId(peerId) {
    if (!isChannelPeerId(peerId)) {
        return null;
    }
    return ('' + peerId).substring(CHANNEL_PEER_PREFIX.length);
}

// 页面加载完成后
window.onload = function () {
    init();
    settings();
    bindRecordSearchEvents();
    bindAdminEvents();
    // 页面加载完成监听回车事件
    document.getElementById("content").addEventListener("keydown", function (e) {
        if (e.keyCode != 13) return;
        e.preventDefault();
        // 发送信息
        sendToChatRoom();
    });
    // 监听窗口切换
    document.addEventListener("visibilitychange", function () {
        if (document.visibilityState === "hidden") {
            // 窗口不可见
            visible = false;
        } else if (document.visibilityState === "visible") {
            // 窗口可见
            visible = true;
            newMsgTotal = 0;
            document.title = title;
        }
    });
    // 请求获取消息通知权限
    requestNoticePermission();
}

// 监听窗口关闭事件，当窗口关闭时，主动去关闭stomp连接
window.onbeforeunload = disconnect;

/**
 * 连接服务器，订阅相关地址
 */
function connect() {
    var socket = new SockJS('/xechat');
    stompClient = Stomp.over(socket);
    // 配置stomp
    config();
    // 订阅地址
    sub();
}

/**
 * 订阅地址
 */
function sub() {
    var user = createUser();
    stompClient.connect(user, function (frame) {
        uid = frame.headers['user-name'];

        if (uid === undefined) {
            alert("进入聊天室失败，请重新连接！");
            refresh();
        }

        user.userId = uid;
        cacheUser(user);

        // 本地订阅
        stompClient.subscribe('/user/' + uid + '/chat', function (data) {
            handleMessage(getData(data.body));
        });

        // 错误信息订阅
        stompClient.subscribe('/user/' + uid + '/error', function (data) {
            var obj = JSON.parse(data.body);
            codeMapping(obj);
            if (obj.code === 504 || obj.code === 505) {
                disconnect();
            }
        });

        // 聊天室动态订阅
        stompClient.subscribe('/topic/status', function (data) {
            var obj = getData(data.body);
            handleMessage(obj);
            showOnlineNum(obj.onlineCount);
            showUserList(obj.onlineUserList);
        });

        initChannel();
        loadFriendList();
        setConnected(true);
    }, function (error) {
        alert('请重新连接！');
        refresh();
    });

}

/**
 * 解析响应数据
 * @param data
 * @returns {*}
 */
function getData(data) {
    var obj = JSON.parse(data);
    codeMapping(obj);
    return obj.data;
}

/**
 * stomp配置
 */
function config() {
    // 每隔30秒做一次心跳检测
    stompClient.heartbeat.outgoing = 30000;
    // 客户端不接收服务器的心跳检测
    stompClient.heartbeat.incoming = 0;
}

/**
 * 关闭连接
 */
function disconnect() {
    if (stompClient !== null) {
        setConnected(false);
        stompClient.disconnect();
        refresh();
    }
}

/**
 * 设置连接状态
 * @param connected true连接成功，false连接失败
 */
function setConnected(connected) {
    showChatRoom(connected);
}

/**
 * 发送信息到指定地址
 * @param pub 发布地址
 * @param header 设置请求头
 * @param data 发送的内容
 */
function sendMessage(pub, header, data) {
    stompClient.send(pub, header, data);
}

/**
 * 生成头像列表
 */
function showHeadPortrait() {
    for (var i = 0; i < head_num; i++) {
        $('.avatar_list_div').append('<img src=./images/avatar/' + i + '.jpeg />');
    }
    $('.avatar_list_div img').bind('click', function () {
        $('#avatarList').attr('src', $(this).attr('src'));
    });
}

/**
 * 发送信息到聊天室
 */
function sendToChatRoom() {
    // 获取发送的内容
    var content = $("#content").val();
    // 内容不能为空
    if (content.trim().length < 1) {
        return;
    }

    var data;
    if (activeChatUserId !== null && !isChannelPeerId(activeChatUserId)) {
        data = JSON.stringify({
            "message": content,
            "receiver": [uid, activeChatUserId]
        });
        sendMessage('/chat', {}, data);
    } else {
        data = JSON.stringify({
            "message": content
        });
        sendMessage('/channel/' + activeChannelId, {}, data);
    }

    $('#content').val('');
    changeBtn();
}

/**
 * 响应码映射
 * @param date
 */
function codeMapping(date) {
    switch (date.code) {
        case 200:
            break;
        case 404:
            alert("404");
            break;
        default:
            alert(date.desc);
            break;
    }
}

/**
 * 转义 防止html注入
 * @param str
 * @returns {string}
 */
function htmlEncode(str) {
    var ele = document.createElement("span");
    ele.appendChild(document.createTextNode(str));
    return ele.innerHTML;
}

/**
 * 显示在线人数
 * @param num
 */
function showOnlineNum(num) {
    $("#online_num").html(num);
}

/**
 * 聊天室界面显隐
 * @param isShow
 */
function showChatRoom(isShow) {
    if (isShow) {
        $("#login").hide();
        $("#showChat").show();
        return;
    }
    $("#login").show();
    $("#showChat").hide();
}

/**
 * 创建用户
 * @returns {string}
 */
function createUser() {
    var username = '匿名';
    var avatar = './images/avatar/1.jpeg';
    var userId;
    var user = getUser();

    if (user !== '') {
        username = user.username;
        avatar = user.avatar;
        userId = user.userId;
    } else {
        var inputName = $('#username').val();
        var inputAvatar = $('#avatarList').attr('src');
        if (inputName.trim() !== '' && inputName.trim().length < 9) {
            username = htmlEncode(inputName);
        }
        if (inputAvatar !== undefined || inputAvatar !== '') {
            avatar = inputAvatar
        }
    }

    var result = {
        'username': username,
        'avatar': avatar,
        'address': address
    };
    if (userId !== undefined && userId !== null && ('' + userId).trim() !== '') {
        result.userId = userId;
    }
    return result;
}

/**
 * 显示用户消息
 * @param data
 */
function showUserMsg(data) {
    if (!data.receiver || data.receiver.length === 0) {
        var channelId = data.channelId ? ('' + data.channelId).trim() : '';
        if (channelId === '') {
            return;
        }
        var peerId = channelPeerId(channelId);
        cacheChatMessage(peerId, data);
        if (!isCurrentChat(peerId)) {
            increaseChannelUnread(channelId);
            return;
        }
        appendChatHtml(buildMessageHtml(data));
        return;
    }

    var user = data.user;
    var isMe = user.userId === uid;
    var peerId = getPeerId(data);
    if (peerId === null) {
        return;
    }

    cacheChatMessage(peerId, data);

    if (!isMe && peerId !== activeChatUserId) {
        increaseUnread(peerId);
        return;
    }
    appendChatHtml(buildMessageHtml(data));
}

/**
 * 跳到聊天界面最底下
 */
function jumpToLow() {
    var showContent = $("#show_content");
    showContent.scrollTop(showContent[0].scrollHeight);
}

/**
 * 处理消息
 * @param data
 */
function handleMessage(data) {
    var msg = data.message;
    switch (data.type) {
        case 'USER':
            showUserMsg(data);
            break;
        case 'SYSTEM':
            if (msg && ('' + msg).indexOf('[FRIEND_REFRESH]') === 0) {
                msg = ('' + msg).replace('[FRIEND_REFRESH]', '');
                loadFriendList();
            }
            if (activeChatUserId !== null) {
                showSystemMsg(msg);
            }
            break;
        case 'REVOKE':
            showRevokeMsg(data);
            break;
        case 'ROBOT':
            showRobotMsg(data);
            break;
        default:
            break;
    }

    // 消息通知
    msgNotice(data);
}

/**
 * 显示撤回消息信息
 * @param data
 */
function showRevokeMsg(data) {
    var peerId = getPeerId(data);
    if (peerId !== null && !isCurrentChat(peerId)) {
        removeMessageFromCache(peerId, data.revokeMessageId);
        return;
    }
    var obj = document.getElementById(data.revokeMessageId);
    if (obj) {
        obj.remove();
        if (peerId !== null) {
            removeMessageFromCache(peerId, data.revokeMessageId);
        }
        showSystemMsg(data.user.username + '撤回了一条消息！');
        jumpToLow();
    }
}

/**
 * 显示系统消息
 * @param message
 */
function showSystemMsg(message) {
    var li = '<li><div class="sys_message">' + message + '</div></li>';
    appendChatHtml(li);
}


/**
 * 撤消消息
 * @param messageId
 */
function revokeMessage(e) {
    var dom = $(e).parents('li');
    var messageId = dom.attr('id');
    var receiver = null;

    if (messageId === '' || messageId === undefined || !confirm('确定撤回这条消息吗？')) {
        return;
    }

    if (activeChatUserId !== null && !isChannelPeerId(activeChatUserId)) {
        receiver = [uid, activeChatUserId];
    }

    var data = JSON.stringify({
        'messageId': messageId,
        'receiver': receiver
    });

    if (receiver) {
        sendMessage('/chatRoom/revoke', {}, data);
        return;
    }

    if (activeChatUserId !== null && isChannelPeerId(activeChatUserId)) {
        var channelId = peerIdToChannelId(activeChatUserId);
        sendMessage('/channel/' + channelId + '/revoke', {}, data);
    }
}

/**
 * 控制按钮显示
 */
function changeBtn() {
    // 如果输入的内容不为空，则展示发送按钮，否则展示上传图片按钮
    if ($('#content').val().trim() != '') {
        $('#send_btn').show();
        $('#picture_btn').hide();
    } else {
        $('#picture_btn').show();
        $('#send_btn').hide();
    }
}

/**
 * 发送图片
 */
function sendImage() {
    var image = $("#file").val();
    if (image === '' || image === undefined) {
        return;
    }

    var filename = image.replace(/.*(\/|\\)/, "");
    var fileExt = (/[.]/.exec(filename)) ? /[^.]+$/.exec(filename.toUpperCase()) : '';

    var file = $('#file').get(0).files[0];
    var fileSize = file.size;
    var mb = 30;
    var maxSize = mb * 1024 * 1024;

    if (fileExt != 'PNG' && fileExt != 'GIF' && fileExt != 'JPG' && fileExt != 'JPEG' && fileExt != 'BMP') {
        alert('发送失败，图片格式有误！');
        return;
    } else if (parseInt(fileSize) > parseInt(maxSize)) {
        alert('上传的图片不能超过' + mb + 'MB');
        return;
    } else {
        var data = new FormData();
        data.append('file', file);
        $.ajax({
            url: "/api/upload/image",
            type: 'POST',
            data: data,
            dataType: 'JSON',
            cache: false,
            processData: false,
            contentType: false,
            success: function (data) {
                codeMapping(data);
                var rep = data.data;
                sendImageToChatRoom(rep.path);
            }
        });
    }

    // 清空选择的文件
    $("#file").val('');
}

/**
 * 选择文件
 */
function selectFile() {
    $('#file').click();
}

/**
 * 发送图片到聊天室
 */
function sendImageToChatRoom(image) {
    var data;
    if (activeChatUserId !== null && !isChannelPeerId(activeChatUserId)) {
        data = JSON.stringify({
            "image": image,
            "receiver": [uid, activeChatUserId]
        });
        sendMessage('/chat', {}, data);
    } else {
        data = JSON.stringify({
            "image": image
        });
        sendMessage('/channel/' + activeChannelId, {}, data);
    }
}

/**
 * 用户列表
 * @param data
 */
function showUserList(data) {
    onlineUserList = data;
    var onlineIds = {};
    onlineUserMap = {};
    $('#onlineUserList').html('');

    for (var i = 0; i < data.length; i++) {
        var obj = data[i];
        onlineUserMap[obj.userId] = obj;
        userMap[obj.userId] = obj;
        onlineIds[obj.userId] = true;
        if (obj.userId === uid) {
            continue;
        }

        var active = activeChatUserId === obj.userId ? 'active' : '';
        var unread = unreadCount[obj.userId] || 0;
        var unreadHtml = unread > 0 ? '<span class="unread-badge">' + unread + '</span>' : '';

        $('#onlineUserList').append('<li id="user_' + obj.userId + '" class="' + active + '" onclick="selectChatUser(\'' + obj.userId + '\')">' +
            '<div class="user-row"><img class="img-responsive avatar_list" src="' + obj.avatar + '">' +
            '<div class="name_list">' + obj.username + '</div>' + unreadHtml + '</div></li>');
    }

    var offlinePeerIds = {};
    if (activeChatUserId !== null && !isChannelPeerId(activeChatUserId) && !onlineIds[activeChatUserId]) {
        offlinePeerIds[activeChatUserId] = true;
    }
    for (var key in chatHistory) {
        if (!chatHistory.hasOwnProperty(key)) {
            continue;
        }
        if (key === uid || isChannelPeerId(key)) {
            continue;
        }
        if (!onlineIds[key]) {
            offlinePeerIds[key] = true;
        }
    }
    for (var k in unreadCount) {
        if (!unreadCount.hasOwnProperty(k)) {
            continue;
        }
        if (k === uid || isChannelPeerId(k)) {
            continue;
        }
        if (!onlineIds[k] && unreadCount[k] > 0) {
            offlinePeerIds[k] = true;
        }
    }

    var offlineList = [];
    for (var peerId in offlinePeerIds) {
        if (offlinePeerIds.hasOwnProperty(peerId)) {
            offlineList.push(peerId);
        }
    }

    for (var j = 0; j < offlineList.length; j++) {
        var offId = offlineList[j];
        var offUser = userMap[offId];
        if (offUser === undefined) {
            continue;
        }
        var offActive = activeChatUserId === offId ? 'active' : '';
        var offUnread = unreadCount[offId] || 0;
        var offUnreadHtml = offUnread > 0 ? '<span class="unread-badge">' + offUnread + '</span>' : '';
        $('#onlineUserList').append('<li id="user_' + offId + '" class="' + offActive + ' offline" onclick="selectChatUser(\'' + offId + '\')">' +
            '<div class="user-row"><img class="img-responsive avatar_list" src="' + offUser.avatar + '">' +
            '<div class="name_list">' + offUser.username + '(离线)</div>' + offUnreadHtml + '</div></li>');
    }
}

/**
 * 退出
 */
function exit() {
    if (confirm('确定退出吗？')) {
        disconnect();
    }
}

/**
 * 缓存用户信息
 * @param data
 */
function cacheUser(data) {
    Cookies.set('user', data);
}

/**
 * 获取用户信息
 * @returns {*}
 */
function getUser() {
    var data = Cookies.get('user');
    if (data !== undefined) {
        return JSON.parse(data);
    }
    return '';
}

/**
 * 初始化登陆信息
 */
function init() {
    // 定位
    getAddress();
    var user = getUser();
    if (user !== '' && user.userId && ('' + user.userId).trim() !== '') {
        $('#account').hide();
        $('#password').hide();
        $('#register').hide();
        $('#username').hide();
        $('.avatar_list_div').remove();
        $('#avatarList').attr('src', user.avatar);
        $('.login-name').html(user.username);
        $('#logout').bind('click', logout);
        $('#logout').show();
    } else {
        if (user !== '' && (!user.userId || ('' + user.userId).trim() === '')) {
            Cookies.remove('user');
        }
        // 初始化头像单选框
        showHeadPortrait();
    }
    $('#joinChat').bind('click', function () {
        $(this).button('loading');
        authLoginThenConnect();
    });
    $('#register').bind('click', function () {
        $(this).button('loading');
        authRegisterThenConnect();
    });
}

/**
 * 注销
 */
function logout() {
    Cookies.remove('user');
    refresh();
}

function authLoginThenConnect() {
    var account = $('#account').val();
    var password = $('#password').val();
    if (account == null || ('' + account).trim() === '' || password == null || ('' + password).trim() === '') {
        $('#joinChat').button('reset');
        alert('请输入账号和密码');
        return;
    }

    $.ajax({
        url: '/api/auth/login',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({account: ('' + account).trim(), password: '' + password}),
        success: function (resp) {
            $('#joinChat').button('reset');
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '登录失败');
                return;
            }
            if (!resp.data || !resp.data.userId) {
                alert('登录失败');
                return;
            }
            cacheUser(resp.data);
            connect();
        },
        error: function () {
            $('#joinChat').button('reset');
            alert('网络异常');
        }
    });
}

function authRegisterThenConnect() {
    var account = $('#account').val();
    var password = $('#password').val();
    var username = $('#username').val();
    var avatar = $('#avatarList').attr('src');
    if (account == null || ('' + account).trim() === '' || password == null || ('' + password).trim() === '') {
        $('#register').button('reset');
        alert('请输入账号和密码');
        return;
    }
    if (username == null || ('' + username).trim() === '') {
        $('#register').button('reset');
        alert('请输入昵称');
        return;
    }

    $.ajax({
        url: '/api/auth/register',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({
            account: ('' + account).trim(),
            password: '' + password,
            username: htmlEncode(('' + username).trim()),
            avatar: avatar,
            address: address
        }),
        success: function (resp) {
            $('#register').button('reset');
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '注册失败');
                return;
            }
            if (!resp.data || !resp.data.userId) {
                alert('注册失败');
                return;
            }
            cacheUser(resp.data);
            connect();
        },
        error: function () {
            $('#register').button('reset');
            alert('网络异常');
        }
    });
}

function getStoredChannelId() {
    return null;
}

function setStoredChannelId(channelId) {
    return;
}

function initChannel() {
    joinedChannelIds = {};
    channelSubscriptions = {};
    channelUnreadCount = {};
    joinChannel('lobby', true, true);
}

function openChannelModal() {
    $('#channelIdInput').val(activeChannelId);
    $('#currentChannelText').text(activeChannelId);
    $('#channelModal').modal('show');
}

function joinChannelFromInput() {
    var channelId = $('#channelIdInput').val();
    joinChannel(channelId, false, true);
}

function createChannelFromInput() {
    var channelId = $('#channelIdInput').val();
    if (channelId == null || ('' + channelId).trim() === '') {
        alert('请输入频道ID');
        return;
    }
    $.ajax({
        url: '/api/channel/create',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({channelId: ('' + channelId).trim()}),
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '创建失败');
                return;
            }
            joinChannel(channelId, false, true);
        },
        error: function () {
            alert('网络异常');
        }
    });
}

function joinChannel(channelId, silent, setActive) {
    if (channelId == null || ('' + channelId).trim() === '') {
        if (!silent) {
            alert('请输入频道ID');
        }
        return;
    }
    $.ajax({
        url: '/api/channel/join',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({channelId: ('' + channelId).trim()}),
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                if (!silent) {
                    alert(resp && resp.desc ? resp.desc : '加入失败');
                }
                return;
            }
            var id = (resp.data && resp.data.channelId) ? ('' + resp.data.channelId).trim() : ('' + channelId).trim();
            if (id === '') {
                return;
            }
            joinedChannelIds[id] = true;
            if (channelSubscriptions[id] == null && stompClient) {
                channelSubscriptions[id] = stompClient.subscribe('/topic/channel/' + id, function (data) {
                    var payload = getData(data.body);
                    if (payload && (!payload.channelId || ('' + payload.channelId).trim() === '')) {
                        payload.channelId = id;
                    }
                    handleMessage(payload);
                });
            }
            renderChannelList();
            if (setActive) {
                selectChannel(id);
            }
            $('#channelModal').modal('hide');
        },
        error: function () {
            if (!silent) {
                alert('网络异常');
            }
        }
    });
}

function renderChannelList() {
    var list = [];
    for (var key in joinedChannelIds) {
        if (joinedChannelIds.hasOwnProperty(key)) {
            list.push(key);
        }
    }
    list.sort();

    var ul = $('#channelList');
    ul.html('');

    for (var i = 0; i < list.length; i++) {
        var channelId = list[i];
        var peerId = channelPeerId(channelId);
        var active = (activeChatUserId === peerId) ? 'active' : '';
        var unread = channelUnreadCount[channelId] || 0;
        var unreadHtml = unread > 0 ? '<span class="unread-badge">' + unread + '</span>' : '';
        ul.append('<li class="' + active + '" onclick="selectChannel(\'' + channelId + '\')">' +
            '<div class="row"><div class="name"># ' + channelId + '</div>' + unreadHtml + '</div></li>');
    }
}

function isFriend(userId) {
    if (userId == null) {
        return false;
    }
    return friendMap[('' + userId).trim()] === true;
}

function loadFriendList() {
    if (uid == null || ('' + uid).trim() === '') {
        return;
    }
    $.ajax({
        url: '/api/friend/list',
        type: 'GET',
        data: {userId: uid},
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                return;
            }
            var list = (resp.data && resp.data.list) ? resp.data.list : [];
            friendMap = {};
            for (var i = 0; i < list.length; i++) {
                var u = list[i];
                if (!u || !u.userId) {
                    continue;
                }
                friendMap[u.userId] = true;
                userMap[u.userId] = u;
            }
            renderFriendList();
        }
    });
}

function renderFriendList() {
    var ul = $('#friendList');
    if (!ul || ul.length === 0) {
        return;
    }
    var list = [];
    for (var key in friendMap) {
        if (friendMap.hasOwnProperty(key) && friendMap[key] === true) {
            list.push(key);
        }
    }
    ul.html('');
    for (var i = 0; i < list.length; i++) {
        var fid = list[i];
        var user = userMap[fid];
        if (!user) {
            continue;
        }
        var active = (activeChatUserId === fid) ? 'active' : '';
        var unread = unreadCount[fid] || 0;
        var unreadHtml = unread > 0 ? '<span class="unread-badge">' + unread + '</span>' : '';
        ul.append('<li class="' + active + '" onclick="selectChatUser(\'' + fid + '\')">' +
            '<div class="row"><div class="name">' + user.username + '</div>' + unreadHtml + '</div></li>');
    }
}

function openAddFriendModal() {
    $('#friendUsernameInput').val('');
    $('#addFriendModal').modal('show');
}

function addFriendFromInput() {
    var name = $('#friendUsernameInput').val();
    if (name == null || ('' + name).trim() === '') {
        alert('请输入对方昵称');
        return;
    }
    if (uid == null || ('' + uid).trim() === '') {
        alert('请先连接聊天室');
        return;
    }
    $.ajax({
        url: '/api/friend/add',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({userId: uid, friendUsername: ('' + name).trim()}),
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                if (resp && resp.code === 502) {
                    alert('请先使用账号登录后再添加好友');
                    return;
                }
                alert(resp && resp.desc ? resp.desc : '添加失败');
                return;
            }
            $('#addFriendModal').modal('hide');
            loadFriendList();
        },
        error: function () {
            alert('网络异常');
        }
    });
}

function bindAdminEvents() {
    $(document).on('keydown', '#adminPassword', function (e) {
        if (e.keyCode === 13) {
            adminLogin();
        }
    });
}

function openAdminModal() {
    $('#adminModal').modal('show');
    adminSyncView();
    if (getAdminToken()) {
        adminLoadUsers();
    }
}

function getAdminToken() {
    try {
        return window.localStorage.getItem(ADMIN_TOKEN_KEY);
    } catch (e) {
        return null;
    }
}

function setAdminToken(token) {
    try {
        if (token == null || ('' + token).trim() === '') {
            window.localStorage.removeItem(ADMIN_TOKEN_KEY);
        } else {
            window.localStorage.setItem(ADMIN_TOKEN_KEY, token);
        }
    } catch (e) {
    }
}

function adminSyncView() {
    var token = getAdminToken();
    if (token) {
        $('#adminPanelView').show();
        $('#adminLogoutBtn').show();
        $('#adminLoginStatus').text('已登录');
    } else {
        $('#adminPanelView').hide();
        $('#adminLogoutBtn').hide();
        $('#adminLoginStatus').text('未登录');
    }
}

function adminLogout() {
    setAdminToken(null);
    $('#adminUserTbody').empty();
    adminSyncView();
}

function adminLogin() {
    var username = $('#adminUsername').val();
    var password = $('#adminPassword').val();
    if (username == null || ('' + username).trim() === '' || password == null || ('' + password).trim() === '') {
        alert('请输入用户名和密码');
        return;
    }

    $.ajax({
        url: '/api/admin/login',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify({username: ('' + username).trim(), password: '' + password}),
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '登录失败');
                return;
            }
            var token = resp.data ? resp.data.token : null;
            if (!token) {
                alert('登录失败');
                return;
            }
            setAdminToken(token);
            $('#adminPassword').val('');
            adminSyncView();
            adminLoadUsers();
        },
        error: function () {
            alert('网络异常');
        }
    });
}

function adminRequest(options) {
    var token = getAdminToken();
    if (!token) {
        alert('请先登录管理员账号');
        return;
    }
    if (!options) {
        return;
    }
    options.headers = options.headers || {};
    options.headers['X-Admin-Token'] = token;

    var originalSuccess = options.success;
    options.success = function (resp) {
        if (resp && resp.code === 502) {
            setAdminToken(null);
            adminSyncView();
            alert(resp.desc || '没有权限');
            return;
        }
        if (originalSuccess) {
            originalSuccess(resp);
        }
    };

    var originalError = options.error;
    options.error = function () {
        if (originalError) {
            originalError();
        } else {
            alert('网络异常');
        }
    };

    $.ajax(options);
}

function adminLoadUsers() {
    var keyword = $('#adminUserKeyword').val();
    adminRequest({
        url: '/api/admin/users',
        type: 'GET',
        data: {keyword: keyword},
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '加载失败');
                return;
            }
            renderAdminUsers(resp.data || []);
        }
    });
}

function renderAdminUsers(users) {
    var tbody = $('#adminUserTbody');
    tbody.empty();
    if (!users || users.length === 0) {
        tbody.append('<tr><td colspan="6" class="text-center text-muted">暂无在线用户</td></tr>');
        return;
    }
    for (var i = 0; i < users.length; i++) {
        var u = users[i] || {};
        var userId = u.userId || '';
        var safeUserId = ('' + userId).replace(/'/g, '');
        var username = htmlEncode(u.username || '');
        var addressText = htmlEncode(u.address || '');
        var statusText = (u.status === 1) ? '在线' : '离线';
        var accountText = u.disabled ? '<span class="label label-danger">已禁用</span>' : '<span class="label label-success">正常</span>';
        var avatar = u.avatar || '';

        var actionHtml = '<div class="admin-actions">' +
            '<button class="btn btn-warning btn-xs" type="button" onclick="adminKick(\'' + safeUserId + '\')">强制下线</button>';
        if (u.disabled) {
            actionHtml += '<button class="btn btn-success btn-xs" type="button" onclick="adminEnable(\'' + safeUserId + '\')">恢复</button>';
        } else {
            actionHtml += '<button class="btn btn-danger btn-xs" type="button" onclick="adminDisable(\'' + safeUserId + '\')">禁用</button>';
        }
        actionHtml += '</div>';

        tbody.append('<tr>' +
            '<td><img class="admin-user-avatar" src="' + avatar + '"/></td>' +
            '<td>' + username + '</td>' +
            '<td>' + addressText + '</td>' +
            '<td>' + statusText + '</td>' +
            '<td>' + accountText + '</td>' +
            '<td>' + actionHtml + '</td>' +
            '</tr>');
    }
}

function adminDisable(userId) {
    if (!confirm('确定禁用该用户吗？')) {
        return;
    }
    adminRequest({
        url: '/api/admin/users/' + userId + '/disable',
        type: 'POST',
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '操作失败');
                return;
            }
            adminLoadUsers();
        }
    });
}

function adminEnable(userId) {
    adminRequest({
        url: '/api/admin/users/' + userId + '/enable',
        type: 'POST',
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '操作失败');
                return;
            }
            adminLoadUsers();
        }
    });
}

function adminKick(userId) {
    if (!confirm('确定强制该用户下线吗？')) {
        return;
    }
    adminRequest({
        url: '/api/admin/users/' + userId + '/kick',
        type: 'POST',
        success: function (resp) {
            if (!resp || resp.code !== 200) {
                alert(resp && resp.desc ? resp.desc : '操作失败');
                return;
            }
            adminLoadUsers();
        }
    });
}

/**
 * 刷新
 */
function refresh() {
    window.location.reload();
}

/**
 * 显示@用户列表
 * @param str
 */
function selectChatUser(userId) {
    if (userId === null || userId === undefined || userMap[userId] === undefined) {
        return;
    }
    if (!isFriend(userId)) {
        alert('请先添加好友再私聊');
        return;
    }
    activeChatUserId = userId;
    unreadCount[userId] = 0;
    renderUserList();
    renderChatWindow(userId);
}

function selectChannel(channelId) {
    if (channelId == null || ('' + channelId).trim() === '') {
        return;
    }
    var id = ('' + channelId).trim();
    activeChannelId = id;
    activeChatUserId = channelPeerId(id);
    channelUnreadCount[id] = 0;
    renderUserList();
    renderChatWindow(activeChatUserId);
}

function renderUserList() {
    renderChannelList();
    renderFriendList();
    showUserList(onlineUserList || []);
}

function renderChatWindow(peerId) {
    if (isChannelPeerId(peerId)) {
        var channelId = peerIdToChannelId(peerId);
        $('#chatTargetTitle').html('当前会话：频道 ' + channelId);
        $('#content').attr('placeholder', '发送到频道 ' + channelId + ' 的消息');
        $('#show_content').html('');

        var list = chatHistory[peerId] || [];
        for (var j = 0; j < list.length; j++) {
            appendChatHtml(buildMessageHtml(list[j]));
        }
        return;
    }

    var user = userMap[peerId];
    if (user === undefined) {
        return;
    }

    $('#chatTargetTitle').html('当前会话：' + user.username);
    $('#content').attr('placeholder', '发送给 ' + user.username + ' 的消息');
    $('#show_content').html('');

    var list = chatHistory[peerId] || [];
    for (var i = 0; i < list.length; i++) {
        appendChatHtml(buildMessageHtml(list[i]));
    }
}

function checkChatTarget() {
    if (activeChatUserId === null) {
        alert('请先从左侧在线用户列表中选择一个私聊对象');
        return false;
    }
    return true;
}

function getPeerId(data) {
    if (!data || !data.user) {
        return null;
    }

    var senderId = data.user.userId;
    if (data.type === 'ROBOT') {
        if (data.channelId && ('' + data.channelId).trim() !== '') {
            return channelPeerId(('' + data.channelId).trim());
        }
        return null;
    }
    if (data.type === 'REVOKE' && (!data.receiver || data.receiver.length === 0)) {
        if (data.channelId && ('' + data.channelId).trim() !== '') {
            return channelPeerId(('' + data.channelId).trim());
        }
        return null;
    }

    if (data.receiver && data.receiver.length > 0) {
        for (var i = 0; i < data.receiver.length; i++) {
            if (data.receiver[i] !== uid) {
                return data.receiver[i];
            }
        }
        if (senderId !== uid) {
            return senderId;
        }
    }

    return senderId === uid ? null : senderId;
}

function isCurrentChat(peerId) {
    return activeChatUserId !== null && peerId === activeChatUserId;
}

function cacheChatMessage(peerId, data) {
    if (!chatHistory[peerId]) {
        chatHistory[peerId] = [];
    }
    chatHistory[peerId].push(data);
}

function removeMessageFromCache(peerId, messageId) {
    if (!chatHistory[peerId]) {
        return;
    }

    chatHistory[peerId] = chatHistory[peerId].filter(function (item) {
        return item.messageId !== messageId;
    });
}

function increaseUnread(peerId) {
    unreadCount[peerId] = (unreadCount[peerId] || 0) + 1;
    renderUserList();
}

function increaseChannelUnread(channelId) {
    channelUnreadCount[channelId] = (channelUnreadCount[channelId] || 0) + 1;
    renderChannelList();
}

function appendChatHtml(html) {
    $("#show_content").append(html);
    jumpToLow();
}

function buildMessageHtml(data) {
    if (data.type === 'SYSTEM') {
        return '<li><div class="sys_message">' + data.message + '</div></li>';
    }

    var user = data.user;
    var isMe = user.userId === uid;
    var style_css = isMe ? 'even' : 'odd';
    var event = isMe ? 'ondblclick=revokeMessage(this)' : '';
    var event2 = isMe ? '' : 'ondblclick=selectChatUser("' + user.userId + '")';
    var showMessage = data.message == null ? '' : htmlEncode(data.message);
    var showImage = data.image == null ? '' : '<div class="show_image"><img src="' + data.image + '"/></div>';
    var li = '<li class=' + style_css + ' id=' + data.messageId + ' data-receiver=' + data.receiver + '>';
    var a = '<a class="user" ' + event2 + '>';
    var avatar = '<img class="img-responsive avatar_" src=' + user.avatar + '\>';
    var span = '<span class="user-name">' + user.username + '</span></a>';
    var div_me = '<div class="reply-content-box"><span class="reply-time"><i class="glyphicon glyphicon-time"></i> '
        + data.sendTime + '&nbsp;<i class="glyphicon glyphicon-map-marker"></i>' + user.address + '</span>';
    var div = '<div class="reply-content-box"><span class="reply-time"><i class="glyphicon glyphicon-map-marker"></i>'
        + user.address + '&nbsp;<i class="glyphicon glyphicon-time"></i> ' + data.sendTime + '</span>';
    var div2 = '<div class="reply-content pr" ' + event + '><span class="arrow">&nbsp;</span>' + showMessage + showImage + '</div></div></li>';

    return li + a + avatar + span + (isMe ? div_me : div) + div2;
}

/**
 * 定位
 */
function getAddress() {
    $.ajax({
        type: "GET",
        url: "https://api.map.baidu.com/location/ip",
        data: {ak: "mHGeNrAuzZscixVMAjfaq1PPgKPOnoPm"},
        dataType: "jsonp",
        async: false,
        success: function (data) {
            address = data.content.address;
        },
        error: function (e) {
            console.log("定位失败！", e);
        }
    });
}

var token;

/**
 * 绑定聊天页聊天记录搜索事件
 */
function bindRecordSearchEvents() {
    if ($('#recordSearchKeyword').length > 0) {
        $('#recordSearchKeyword').on('keydown', function (e) {
            if (e.keyCode === 13) {
                e.preventDefault();
                searchRecordFromChat();
            }
        });
    }
}

/**
 * 打开聊天记录查询弹窗
 */
function openRecordSearchModal() {
    if ($('#recordSearchModal').length < 1) {
        return;
    }
    $('#recordSearchModal').modal('show');
}

/**
 * 从聊天界面搜索聊天记录
 */
function searchRecordFromChat() {
    var keywordInput = $('#recordSearchKeyword');
    if (keywordInput.length < 1) {
        return;
    }

    var keyword = keywordInput.val().trim();
    if (keyword === '') {
        alert('请输入查询关键词');
        return;
    }

    $.ajax({
        type: "GET",
        url: "/api/record/search",
        data: {
            "keyword": keyword,
            "limit": 100
        },
        dataType: "json",
        success: function (data) {
            codeMapping(data);
            if (data.code === 200) {
                renderChatRecordSearchResult(data.data.list || [], keyword);
            }
        }
    });
}

/**
 * 渲染聊天页聊天记录搜索结果
 */
function renderChatRecordSearchResult(list, keyword) {
    var resultWrap = $('#recordSearchResultWrap');
    var resultList = $('#recordSearchList');
    var content = $('#recordSearchContent');
    if (resultWrap.length < 1 || resultList.length < 1 || content.length < 1) {
        return;
    }

    resultList.html('');
    content.html('');

    if (list.length === 0) {
        resultWrap.show();
        resultList.append('<li>未找到包含“' + htmlEncode(keyword) + '”的记录</li>');
        return;
    }

    for (var i = 0; i < list.length; i++) {
        var item = list[i];
        var sender = item.sender == null || item.sender === '' ? '未知' : item.sender;
        var snippet = item.content == null || item.content === '' ? '-' : htmlEncode(item.content);
        resultList.append('<li data-url="' + item.url + '" onclick="readChatRecordContent(this)">' +
            '第' + item.lineNumber + '行 发送者：' + htmlEncode(sender) + ' 消息：' +
            '<span class="record-search-snippet">' + snippet + '</span></li>');
    }
    resultWrap.show();
}

/**
 * 在聊天页弹窗中读取记录文件详情
 */
function readChatRecordContent(e) {
    var url = $(e).data('url');
    if (url === undefined || url === '') {
        return;
    }

    $.ajax({
        type: "GET",
        url: url,
        cache: false,
        success: function (data) {
            $('#recordSearchContent').html('<pre class="record-content-pre">' + htmlEncode(data) + '</pre>');
        }
    });
}

/**
 * 获取聊天记录列表
 */
function listRecord(name) {
    var def_id = 'recordList';
    var id = '';
    var param = '';
    if (name != '') {
        def_id = name;
        id = name + '_';
        param = id.replace(/\_/g, '/');
    }

    $.ajax({
        type: "GET",
        url: "/api/record",
        headers: {
            "token": token
        },
        data: {
            "directoryName": param
        },
        dataType: "json",
        success: function (data) {
            codeMapping(data);
            if (data.code === 200) {
                $('#' + def_id).html('');
                $('#passwordModel').modal('hide');
                $('#record').show();
                var list = data.data.list;
                for (var i = 0; i < list.length; i++) {
                    var obj = list[i];
                    if (obj.file) {
                        $('#' + def_id).append('<li class="file" onclick="readContent(this)" data-url="' + obj.url + '">' + obj.name + '</li>');
                    } else {
                        var dir_id = id + obj.name;
                        var unit = obj.name.length === 4 ? '年' : '月';
                        $('#' + def_id).append('<li class="dire" onclick="direDisplay(this)" data-id="' + dir_id + '">' + obj.name + unit + '</li>');
                        $('#' + def_id).append('<ul id="' + dir_id + '"></ul>')
                    }
                }
            }
        }
    });
}

/**
 * 搜索聊天记录
 */
function searchRecord() {
    var keyword = $('#searchKeyword').val();
    if (keyword === undefined) {
        return;
    }

    keyword = keyword.trim();
    if (keyword === '') {
        alert('请输入检索关键词');
        return;
    }

    $.ajax({
        type: "GET",
        url: "/api/record/search",
        headers: {
            "token": token
        },
        data: {
            "keyword": keyword,
            "limit": 100
        },
        dataType: "json",
        success: function (data) {
            codeMapping(data);
            if (data.code === 200) {
                $('#record').show();
                renderSearchResult(data.data.list || [], keyword);
            }
        }
    });
}

/**
 * 渲染检索结果
 */
function renderSearchResult(list, keyword) {
    var result = $('#searchResult');
    var resultList = $('#searchResultList');
    resultList.html('');

    if (list.length === 0) {
        result.show();
        resultList.append('<li>未找到包含 “' + htmlEncode(keyword) + '” 的聊天记录</li>');
        return;
    }

    for (var i = 0; i < list.length; i++) {
        var item = list[i];
        var sender = item.sender == null || item.sender === '' ? '未知' : item.sender;
        var snippet = item.content == null || item.content === '' ? '-' : htmlEncode(item.content);
        resultList.append('<li class="file" onclick="readContent(this)" data-url="' + item.url + '">' +
            '第' + item.lineNumber + '行 发送者：' + htmlEncode(sender) + ' 消息：' +
            '<span class="search-snippet">' + snippet + '</span></li>');
    }

    result.show();
}

/**
 * 读取文件内容
 * @param url
 */
function readContent(e) {
    $.ajax({
        type: "GET",
        url: $(e).data('url'),
        headers: {
            "token": token
        },
        cache: false,
        success: function (data) {
            $('#record #content').html(marked(data));
        }
    });
}

/**
 * 校验密码
 */
function checkPassword() {
    var val = $('#record_password').val();
    if (val === '') {
        alert("请输入访问密码！！！");
    } else {
        token = btoa(val);
        $('#searchResult').hide();
        $('#searchResultList').html('');
        listRecord('', '');
    }
}

/**
 * 显示机器人消息
 * @param data
 */
function showRobotMsg(data) {
    var peerId = getPeerId(data);
    if (peerId === null) {
        return;
    }
    cacheChatMessage(peerId, data);
    if (!isCurrentChat(peerId)) {
        increaseUnread(peerId);
        return;
    }
    appendChatHtml(buildMessageHtml(data));
}

/**
 * 目录显示
 *
 * @param id
 */
function direDisplay(e) {
    var id = $(e).data('id');
    if ($('#' + id).is(':hidden')) {
        listRecord(id);
        $('#' + id).show();
        return;
    }
    $('#' + id).hide();
}

/**
 * 不在当前窗口时，通过标题显示最新的消息数量
 */
function msgNoticeByTitle() {
    if (!openNotice || visible) {
        // 未开启通知或窗口可见，不进行提醒
        return;
    }

    if (opendSound) {
        // 提示音
        beep();
    }
    // 窗口不可见显示提醒
    document.title = '[' + (++newMsgTotal) + '条新消息]' + title;
}

/**
 * 通过浏览器的消息通知来推送消息
 * 兼容性差（Safari、Chrome等浏览器对于pc端基本支持）
 * @param data
 */
function msgNoticeByBrowser(data) {
    if (permission == 3) {
        requestNoticePermission();
    }

    if (permission == 1) {
        // 创建通知
        var notice = createNotice(data);
        notice.onclick = function () {
            // 切换浏览器窗口到当前界面
            window.focus();
        }
    }
}

/**
 * 播放提示音
 */
function beep() {
    var beep = document.getElementById('beep');
    beep.play();
}

/**
 * 创建一条消息通知
 * @param data
 * @returns {Notification}
 */
function createNotice(data) {
    var t = '系统消息';
    var msg = data.message;
    var user = data.user;
    var type = data.type;
    if (type == 'USER' || type == 'ROBOT') {
        t = user.username;
        if (msg == null) {
            msg = "[图片]";
        }
    } else if (type == 'REVOKE') {
        msg = user.username + '撤回了一条消息！';
    }

    return new Notification('新的消息！' + title, {
        body: t + '：' + msg,
        icon: user.avatar
    });
}

/**
 * 请求通知权限
 */
function requestNoticePermission() {
    var flag = window.Notification;
    if (flag) {
        Notification.requestPermission(function (perm) {
            switch (perm) {
                case "granted":
                    permission = 1;
                    break;
                case "denied":
                    permission = 2;
                    break;
                default:
                    permission = 3;
                    break;
            }
        });
    } else {
        console.log('该浏览器暂不支持通知！');
        permission = 0;
    }
}

/**
 * 消息通知
 * @param data
 */
function msgNotice(data) {
    // 已开启通知且窗口不可见才进行消息通知
    if (openNotice && !visible) {
        // 通过标题通知
        msgNoticeByTitle();
        // 通过浏览器的消息通知支持进行通知
        msgNoticeByBrowser(data);
    }
}

/**
 * 设置相关
 */
function settings() {
    var checkNotice = $('#checkNotice');
    checkNotice.on('change', function () {
        // 是否打开通知
        openNotice = checkNotice.is(':checked');
    });

    var checkSound = $('#checkSound');
    checkSound.on('change', function () {
        // 是否打开提示音
        opendSound = checkSound.is(':checked');
    });
}
