package cn.xeblog.xechat.domain.ro;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AuthRegisterRO implements Serializable {

    private static final long serialVersionUID = -1932141598549027048L;

    private String account;

    private String password;

    private String username;

    private String avatar;

    private String address;
}
