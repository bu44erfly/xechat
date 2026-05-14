package cn.xeblog.xechat.enums;

import cn.xeblog.xechat.enums.inter.Code;

/**
 * 响应码枚举
 *
 * @author yanpanyi
 * @date 2019/3/20
 */
public enum CodeEnum implements Code {

    /**
     * 上传的文件不是图片
     */
    UPLOADED_FILE_IS_NOT_AN_IMAGE(1002, "上传的文件不是图片!"),
    /**
     * 消息已过期
     */
    MESSAGE_HAS_EXPIRED(1001, "消息已过期，不能撤回！"),
    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "网络异常！"),
    /**
     * 参数验证失败
     */
    INVALID_PARAMETERS(501, "非法参数！"),
    /**
     * Token验证不通过
     */
    INVALID_TOKEN(502, "没有权限！"),
    /**
     * 处理失败
     */
    FAILED(503, "处理失败！"),
    USER_DISABLED(504, "账号已被禁用！"),
    USER_FORCED_OFFLINE(505, "你已被管理员强制下线！"),
    GROUP_DISABLED(506, "群聊已被管理员关闭！"),
    USER_ALREADY_EXISTS(507, "账号已存在！"),
    USER_LOGIN_FAILED(508, "账号或密码错误！"),
    CHANNEL_NOT_FOUND(509, "频道不存在！"),
    CHANNEL_ALREADY_EXISTS(510, "频道已存在！"),
    USERNAME_ALREADY_EXISTS(511, "昵称已存在！"),
    FRIEND_NOT_FOUND(512, "未找到该用户！"),
    FRIEND_ALREADY_ADDED(513, "已经是好友了！"),
    FRIEND_CANNOT_ADD_SELF(514, "不能添加自己为好友！"),
    FRIEND_REQUIRED(515, "请先添加好友再私聊！"),
    /**
     * 响应成功
     */
    SUCCESS(200, "Success");

    private int code;
    private String desc;

    CodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
