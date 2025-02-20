package com.project3.revtech.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project3.revtech.request.LoginRequest;
import com.project3.revtech.request.SignupRequest;
import com.project3.revtech.response.JwtResponse;
import com.project3.revtech.response.MessageResponse;
import com.project3.revtech.service.UserDetailsImpl;
import com.project3.revtech.dao.UserRepository;
import com.project3.revtech.entity.ERole;
import com.project3.revtech.entity.RoleEntity;
import com.project3.revtech.entity.UserEntity;
import com.project3.revtech.dao.RoleRepository;
import com.project3.revtech.security.jwt.JwtUtils;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt, 
            userDetails.getUser_id(), 
            userDetails.getUsername(), 
            userDetails.getEmail(), 
            roles, userDetails.getFirst_name(), userDetails.getLast_name(), userDetails.getAddress(), userDetails.getContact()));
  }
  
  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Email is already in use!"));
    }

    // Create new user's account
    UserEntity userEntity = new UserEntity(signUpRequest.getUsername(), 
               signUpRequest.getEmail(),
               encoder.encode(signUpRequest.getPassword()),signUpRequest.getFirst_name(),signUpRequest.getLast_name(),
               signUpRequest.getAddress(),signUpRequest.getContact());

    Set<String> strRoles = signUpRequest.getRole();
    Set<RoleEntity> roles = new HashSet<>();

    if (strRoles == null) {
      RoleEntity userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          RoleEntity adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "emp":
          RoleEntity modRole = roleRepository.findByName(ERole.ROLE_EMPLOYEE)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          RoleEntity userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    userEntity.setRoles(roles);
    userRepository.save(userEntity);

    return ResponseEntity.ok()
    		.body(new MessageResponse("User registered successfully!"));
  }
}
