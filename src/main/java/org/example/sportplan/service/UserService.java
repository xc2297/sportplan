package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateUserRequest;
import org.example.sportplan.dto.response.UserResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.entity.UserGroup;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.GroupMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务层
 * 处理用户相关的业务逻辑，包括用户注册、登录认证、用户信息查询等。
 * 密码使用 SHA-256 哈希存储，不存储明文密码。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;

    /**
     * 创建新用户（注册）
     * 校验账号是否已存在，对密码进行 SHA-256 哈希后存储。
     *
     * @param request 注册请求，包含账号、密码、姓名、性别
     * @return 用户信息响应
     * @throws BusinessException 当账号已存在时抛出
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 检查账号是否已被注册
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (existing != null) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        // 密码哈希存储，不保存明文
        user.setPassword(hashPassword(request.getPassword()));
        user.setName(request.getName());
        user.setGender(User.Gender.valueOf(request.getGender().toUpperCase()));
        userMapper.insert(user);
        return toResponse(user);
    }

    /**
     * 用户登录认证
     * 根据账号查找用户，比对密码哈希值进行验证。
     * 注意：账号或密码错误时返回相同的提示信息，避免泄露账号是否存在。
     *
     * @param username    登录账号
     * @param rawPassword 明文密码
     * @return 认证成功的用户实体
     * @throws BusinessException 当账号不存在或密码错误时抛出
     */
    public User login(String username, String rawPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        // 比对密码的 SHA-256 哈希值
        if (!user.getPassword().equals(hashPassword(rawPassword))) {
            throw new BusinessException("账号或密码错误");
        }
        return user;
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息响应
     * @throws BusinessException 当用户不存在时抛出
     */
    public UserResponse getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toResponse(user);
    }

    /**
     * 查询所有用户列表
     *
     * @return 所有用户信息响应列表
     */
    public List<UserResponse> getAllUsers() {
        return userMapper.selectList(null).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 将用户实体转换为响应 DTO
     * 过滤敏感信息（如密码），仅返回前端需要展示的字段。
     *
     * @param user 用户实体
     * @return 用户响应 DTO
     */
    private UserResponse toResponse(User user) {
        String groupName = null;
        if (user.getGroupId() != null) {
            UserGroup group = groupMapper.selectById(user.getGroupId());
            groupName = group != null ? group.getName() : null;
        }
        boolean isAdmin = false;
        if (user.getGroupId() != null) {
            UserGroup group = groupMapper.selectById(user.getGroupId());
            isAdmin = Boolean.TRUE.equals(user.getIsGroupAdmin())
                    || (group != null && user.getId().equals(group.getCreatorId()));
        }
        return new UserResponse(user.getId(), user.getName(),
                user.getGender().name().toLowerCase(), isAdmin,
                user.getGroupId(), groupName);
    }

    /**
     * 使用 SHA-256 算法对密码进行哈希
     * 将明文密码转换为不可逆的哈希值，用于安全存储。
     *
     * @param rawPassword 明文密码
     * @return SHA-256 哈希后的十六进制字符串
     */
    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
