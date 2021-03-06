package com.example.seesaw.service;

import com.example.seesaw.dto.*;
import com.example.seesaw.exception.CustomException;
import com.example.seesaw.exception.ErrorCode;
import com.example.seesaw.model.User;
import com.example.seesaw.model.UserProfile;
import com.example.seesaw.model.UserProfileNum;
import com.example.seesaw.model.UserRoleEnum;
import com.example.seesaw.repository.MbtiRepository;
import com.example.seesaw.repository.UserProfileNumRepository;
import com.example.seesaw.repository.UserProfileRepository;
import com.example.seesaw.repository.UserRepository;
import com.example.seesaw.security.jwt.JwtDecoder;
import com.example.seesaw.security.jwt.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileNumRepository userProfileNumRepository;
    private final MbtiRepository mbtiRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;
    private static final String ADMIN_TOKEN = "AAABnv/xRVklrnYxKZ0aHgTBcXukeZygoC";

    public void registerUser(SignupRequestDto requestDto) {

        String username = requestDto.getUsername();
        checkUserName(username);
        String generation = requestDto.getGeneration();
        MbtiRequestDto mbtiRequestDto = new MbtiRequestDto(requestDto.getEnergy(), requestDto.getInsight(), requestDto.getJudgement(), requestDto.getLifePattern());
        String mbti = checkMbti(mbtiRequestDto);
        String nickname = requestDto.getNickname();
        checkNickName(nickname);
        Long postCount = 0L;

        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {
            if (!requestDto.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new IllegalArgumentException("????????? ????????? ?????? ????????? ??????????????????.");
            }
            role = UserRoleEnum.ADMIN;
        }
        //profile ??????
        List<Long> charIds = requestDto.getCharId();
        if (charIds == null){
            throw new IllegalArgumentException("charIds??? null ?????????.");
        }

        // ???????????? ?????????
        String enPassword = passwordEncoder.encode(requestDto.getPwd());
        User user = new User(username, enPassword, nickname, generation, postCount, mbti,role);
        userRepository.save(user); // DB ??????

        for(Long charId : charIds){
            UserProfile userProfile = userProfileRepository.findByCharId(charId);
            UserProfileNum userProfileNum = new UserProfileNum(userProfile, user);
            userProfileNumRepository.save(userProfileNum);
        }

    }

    // ????????? ????????? ??????
    private void checkUserName(String username) {
        Optional<User> foundByUserName = userRepository.findByUsername(username);
        if (foundByUserName.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATED_USER_NAME);
        }
        Pattern userNamePattern = Pattern.compile("\\w+@\\w+\\.\\w+(\\.\\w+)?");
        Matcher userNameMatcher = userNamePattern.matcher(username);
        if (username.length() == 0) {
            throw new CustomException(ErrorCode.BLANK_USER_NAME);
        }
        if (!userNameMatcher.matches()) {
            throw new CustomException(ErrorCode.INVALID_PATTERN_USER_NAME);
        }
    }


    //???????????? ????????? ??????
    private void checkUserPw(String pwd, String pwdCheck) {
        Pattern userPwPattern = Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$ %^&*-]).{8,20}$");
        Matcher userPwMatcher = userPwPattern.matcher(pwd);
        if (pwd.length() == 0) {
            throw new CustomException(ErrorCode.BLANK_USER_PW);
        }
        if (!userPwMatcher.matches()) {
            throw new CustomException(ErrorCode.INVALID_PATTERN_USER_PW);
        }
        // password ????????????
        if (pwdCheck.length() == 0) {
            throw new CustomException(ErrorCode.BLANK_USER_PW_CHECK);
        }
        if (!pwd.equals(pwdCheck)) {
            throw new CustomException(ErrorCode.NOT_EQUAL_USER_PW_CHECK);
        }
    }

    //????????? ????????? ??????
    public String checkNickName(String nickname) {
        Pattern nickNamePattern = Pattern.compile("^\\S{2,8}$");
        Matcher nickNameMatcher = nickNamePattern.matcher(nickname);

        Optional<User> foundByNickName = userRepository.findByNickname(nickname);
        if (foundByNickName.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATED_USER_NICKNAME);
        }
        if (nickname.length() == 0) {
            throw new CustomException(ErrorCode.BLANK_USER_NICKNAME);
        }
        if (!nickNameMatcher.matches()) {
            throw new CustomException(ErrorCode.INVALID_PATTERN_USER_NICKNAME);
        }
        return nickname;
    }

    public String validateToken(RefreshTokenDto refreshTokenDto) {
        String username = jwtDecoder.decodeUsername(refreshTokenDto.getRefreshToken());
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("???????????? ???????????? ????????????."));
        return JwtTokenUtils.generateJwtToken(user);
    }

    public String checkMbti(MbtiRequestDto mbtiRequestDto) {
        String mbtiName = mbtiRequestDto.getEnergy() + mbtiRequestDto.getInsight() + mbtiRequestDto.getJudgement() + mbtiRequestDto.getLifePattern();
        if(mbtiName.length() != 4 || mbtiName.contains("null")){
            throw new CustomException(ErrorCode.BLANK_USER_MBTI);
        }
        //MBTI table ???????????? ?????? ???????????? ???????????? ????????????
        String detail = mbtiRepository.findByMbtiName(mbtiName).getDetail();
        if(detail.isEmpty()){
            throw new IllegalArgumentException("???????????? MBTI??? ????????????.");
        }
        return detail;
    }

    @Transactional
    public UserInfoResponseDto findUserInfo(User user) {
        List<ProfileListDto> profileListDtos = findUserProfiles(user);

        return new UserInfoResponseDto(user.getUsername(), user.getNickname(), profileListDtos);
    }

    public void checkUser(UserCheckRequestDto userCheckRequestDto) {
        String username = userCheckRequestDto.getUsername();
        String pwd = userCheckRequestDto.getPwd();
        String pwdCheck = userCheckRequestDto.getPwdCheck();

        //????????? ????????? ??????
        checkUserName(username);

        //???????????? ????????? ??????
        checkUserPw(pwd, pwdCheck);
    }

    public List<ProfileListDto> findUserProfiles(User user){

        List<UserProfileNum> userProfileNums = userProfileNumRepository.findAllByUserId(user.getId());
        System.out.println("userId   "+ user.getId());
        if(userProfileNums.isEmpty()){
            throw new IllegalArgumentException("????????? userProfileId ??? ????????????.");
        }

        List<ProfileListDto> profileListDtos = new ArrayList<>();
        for(UserProfileNum num : userProfileNums){
            UserProfile userProfile = userProfileRepository.findById(num.getUserProfile().getId()).orElseThrow(
                    () -> new IllegalArgumentException("???????????? userProfile ??? ????????????."));
            if (userProfile.getCategory().equals("faceUrl")){
                ProfileListDto faceUrl = new ProfileListDto(userProfile.getCharId(), userProfile.getImageUrl());
                profileListDtos.add(faceUrl);
            } else if(userProfile.getCategory().equals("accessoryUrl")){
                ProfileListDto accessoryUrl = new ProfileListDto(userProfile.getCharId(), userProfile.getImageUrl());
                profileListDtos.add(accessoryUrl);
            } else if(userProfile.getCategory().equals("backgroundUrl")){
                ProfileListDto backgroundUrl = new ProfileListDto(userProfile.getCharId(), userProfile.getImageUrl());
                profileListDtos.add(backgroundUrl);
            }
        }
        return profileListDtos;
    }
}

