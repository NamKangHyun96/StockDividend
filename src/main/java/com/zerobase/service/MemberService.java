package com.zerobase.service;

import com.zerobase.exception.impl.AlreadyExistUserException;
import com.zerobase.exception.impl.NotFoundUserException;
import com.zerobase.model.Auth;
import com.zerobase.persist.MemberRepository;
import com.zerobase.persist.entity.MemberEntity;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {


    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));

    }

    public MemberEntity register(Auth.SignUp member) {
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        var result = this.memberRepository.save(member.toEntity());
        return result;
    }

    public MemberEntity authenticate(Auth.SignIn member) {
        var user = this.memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new NotFoundUserException());

        if (!this.passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            throw new NotFoundUserException();
        }
        return user;
    }
}
