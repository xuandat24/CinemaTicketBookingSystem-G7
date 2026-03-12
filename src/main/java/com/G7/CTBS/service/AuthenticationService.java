package com.G7.CTBS.service;

import com.G7.CTBS.dto.AuthenticationRequest;
import com.G7.CTBS.dto.IntrospectRequest;
import com.G7.CTBS.dto.AutheticationResponse;
import com.G7.CTBS.dto.IntrospectResponse;
import com.G7.CTBS.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    private final UserRepository userRepository;
    @NonFinal
    @Value("${JWT_TOKEN}")
    private String JWT_TOKEN;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        JWSVerifier verifier = new MACVerifier(JWT_TOKEN.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expireTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        return IntrospectResponse.builder()
                .valid(signedJWT.verify(verifier) && expireTime.after(new Date()))
                .build();
    }

    public AutheticationResponse authenticated(AuthenticationRequest request){
        // 1. Tìm kiếm User: Cho phép đăng nhập bằng cả UserName hoặc Email
        var user = userRepository.findByUserNameOrEmail(request.getUserName(), request.getUserName())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!"));

        // 2. So sánh mật khẩu bằng PasswordEncoder
        // Lưu ý: Mật khẩu trong DB phải được mã hóa BCrypt khi đăng ký
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!authenticated) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        // 3. Tạo JWT Token
        var token = tokenGeneration(user.getUserName());

        // 4. Trả về Response kèm theo userName để Frontend hiển thị
        return AutheticationResponse.builder()
                .token(token)
                .authenticated(true)
                .userName(user.getUserName()) // Đảm bảo DTO AutheticationResponse có trường này
                .build();
    }

    public String tokenGeneration(String username){
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("sums.vn")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try{
            jwsObject.sign(new MACSigner(JWT_TOKEN.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            System.err.println("Cannot create token: "+ e);
            throw new RuntimeException(e);
        }
    }

    public String extractUsername(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getSubject();
    }

}
