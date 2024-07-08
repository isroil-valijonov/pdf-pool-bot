package com.example.pdfpoolbot.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void save(Long userId, String firstName, String userName) {
        User user = new User(userId, firstName, userName, LocalDate.now());
        userRepository.save(user);
    }

    public boolean findByUserId(Long userId) {
        Optional<User> users = userRepository.findById(userId);
        return users.isPresent();
    }

    public List<Long> userIdList() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .map(User::getUserId)
                .collect(Collectors.toList());
    }

    public UserDto getUserActivityInfo() {
        Long lastMonthCount = userRepository.countUsersLastMonth();
        Long thisMonthCount = userRepository.countUsersThisMonth();
        List<User> activeUsersThisMonth = userRepository.findActiveUsersThisMonth();

        Long difference = thisMonthCount - lastMonthCount;
        List<String> usernames = activeUsersThisMonth.stream().map(User::getUserName).collect(Collectors.toList());

        return new UserDto(difference, usernames);
    }

    public UserDto allUsers() {
        List<User> users = userRepository.findAll();
        long count = users.size();
        List<String> usernames = users.stream().map(User::getUserName).toList();
        return new UserDto(count, usernames);
    }

    public UserDto getUserCreationInfo(String date) {
        List<User> users = userRepository.findUsersByCreationDate(date);
        long count = users.size();
        List<String> usernames = users.stream().map(User::getUserName).collect(Collectors.toList());
        return new UserDto(count, usernames);
    }
}
