package cn.xeblog.xechat.domain.ro;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class FriendAddRO implements Serializable {

    private static final long serialVersionUID = 3995750061540643367L;

    private String userId;

    private String friendUsername;
}
