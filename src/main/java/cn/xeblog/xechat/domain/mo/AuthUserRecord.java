package cn.xeblog.xechat.domain.mo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AuthUserRecord implements Serializable {

    private static final long serialVersionUID = 2620849735277255742L;

    private String userId;

    private String account;

    private String passwordMd5;

    private String username;

    private String avatar;

    private String address;

    private Long createdAt;
}
