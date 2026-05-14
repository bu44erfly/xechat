package cn.xeblog.xechat.domain.ro;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AuthLoginRO implements Serializable {

    private static final long serialVersionUID = 5031097024569898931L;

    private String account;

    private String password;
}
