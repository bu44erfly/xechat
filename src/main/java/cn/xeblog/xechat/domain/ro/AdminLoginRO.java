package cn.xeblog.xechat.domain.ro;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AdminLoginRO implements Serializable {

    private static final long serialVersionUID = 4089624012617030119L;

    private String username;

    private String password;
}
