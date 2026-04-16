package com.dna_testing_system.dev.service.user;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.UserProfileMapper;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserProfileMapper userProfileMapper;

    @InjectMocks UserProfileServiceImpl service;

    @Test
    void updateUserProfile_userNotFound_throwsResourceNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserProfile("ghost", UserProfileRequest.builder().email("a@ex.com").build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_EXISTS.getMessage());

        verifyNoInteractions(userProfileRepository, userProfileMapper);
    }

    @Test
    void updateUserProfile_profileNull_createsNewAndPreservesEmailWhenMissingInRequest() {
        User user = User.builder().id("u1").username("alice").passwordHash("x").build();
        user.setProfile(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfileRequest req = UserProfileRequest.builder()
                .firstName("A")
                .lastName("B")
                .email("alice@example.com")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

        boolean result = service.updateUserProfile("alice", req);

        assertThat(result).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProfile()).isNotNull();
        assertThat(captor.getValue().getProfile().getUser()).isSameAs(user);
        verify(userProfileMapper).updateUserProfileFromDto(eq(req), any(UserProfile.class));
    }

    @Test
    void updateUserProfile_emailUnchanged_doesNotThrowAndSaves() {
        UserProfile profile = UserProfile.builder().email("same@ex.com").build();
        User user = User.builder().id("u1").username("alice").passwordHash("x").profile(profile).build();
        profile.setUser(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfileRequest req = UserProfileRequest.builder()
                .email("same@ex.com")
                .firstName("New")
                .build();

        boolean result = service.updateUserProfile("alice", req);

        assertThat(result).isTrue();
        verify(userRepository).save(user);
        verify(userProfileRepository, never()).findAll();
        verify(userProfileMapper).updateUserProfileFromDto(eq(req), eq(profile));
    }

    @Test
    void updateUserProfile_emailChangedButUnique_saves() {
        UserProfile profile = UserProfile.builder().email("old@ex.com").build();
        User user = User.builder().id("u1").username("alice").passwordHash("x").profile(profile).build();
        profile.setUser(user);

        User other = User.builder().id("u2").username("bob").passwordHash("x").build();
        UserProfile otherProfile = UserProfile.builder().email("other@ex.com").user(other).build();
        other.setProfile(otherProfile);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userProfileRepository.existsByEmailIgnoreCaseAndUserIdNot("new@ex.com", "u1")).thenReturn(false);

        UserProfileRequest req = UserProfileRequest.builder()
                .email("new@ex.com")
                .build();

        boolean result = service.updateUserProfile("alice", req);

        assertThat(result).isTrue();
        verify(userRepository).save(user);
        verify(userProfileMapper).updateUserProfileFromDto(eq(req), eq(profile));
    }

    @Test
    void updateUserProfile_emailChangedButDuplicate_throwsRuntimeException() {
        UserProfile profile = UserProfile.builder().email("old@ex.com").build();
        User user = User.builder().id("u1").username("alice").passwordHash("x").profile(profile).build();
        profile.setUser(user);

        User other = User.builder().id("u2").username("bob").passwordHash("x").build();
        UserProfile otherProfile = UserProfile.builder().email("dup@ex.com").user(other).build();
        other.setProfile(otherProfile);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userProfileRepository.existsByEmailIgnoreCaseAndUserIdNot("dup@ex.com", "u1")).thenReturn(true);

        UserProfileRequest req = UserProfileRequest.builder()
                .email("dup@ex.com")
                .build();

        assertThatThrownBy(() -> service.updateUserProfile("alice", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already in use by another user");

        verify(userRepository, never()).save(any());
        verify(userProfileMapper, never()).updateUserProfileFromDto(any(UserProfileRequest.class), any(UserProfile.class));
    }

    @Test
    void getUserProfile_userNotFound_throwsResourceNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserProfile("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_EXISTS.getMessage());
    }

    @Test
    void getUserProfile_mapsToDto() {
        User user = User.builder().id("u1").username("alice").passwordHash("x").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userProfileMapper.toDto(user)).thenReturn(UserProfileResponse.builder().username("alice").build());

        UserProfileResponse result = service.getUserProfile("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        verify(userProfileMapper).toDto(user);
    }

    @Test
    void deleteUserProfile_profileExists_deletesAndNullsProfile() {
        UserProfile profile = UserProfile.builder().email("a@ex.com").build();
        User user = User.builder().id("u1").username("alice").passwordHash("x").profile(profile).build();
        profile.setUser(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        boolean result = service.deleteUserProfile("alice");

        assertThat(result).isTrue();
        verify(userProfileRepository).delete(profile);
        assertThat(user.getProfile()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserProfile_profileNull_doesNotDeleteButReturnsTrue() {
        User user = User.builder().id("u1").username("alice").passwordHash("x").profile(null).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        boolean result = service.deleteUserProfile("alice");

        assertThat(result).isTrue();
        verify(userProfileRepository, never()).delete(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserProfileByName_nullOrBlank_delegatesToGetUserProfiles() {
        User u1 = User.builder().id("u1").username("alice").passwordHash("x").build();
        User u2 = User.builder().id("u2").username("bob").passwordHash("x").build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(userProfileMapper.toDto(u1)).thenReturn(UserProfileResponse.builder().username("alice").build());
        when(userProfileMapper.toDto(u2)).thenReturn(UserProfileResponse.builder().username("bob").build());

        assertThat(service.getUserProfileByName(null)).extracting(UserProfileResponse::getUsername)
                .containsExactly("alice", "bob");
        assertThat(service.getUserProfileByName("   ")).extracting(UserProfileResponse::getUsername)
                .containsExactly("alice", "bob");
    }

    @Test
    void getUserProfileByName_filtersByFirstLastOrFullName_caseInsensitive_andSkipsNullProfiles() {
        UserProfile p1 = UserProfile.builder().firstName("Alice").lastName("Nguyen").email("a@ex.com").build();
        User u1 = User.builder().id("u1").username("alice").passwordHash("x").profile(p1).build();
        p1.setUser(u1);

        UserProfile p2 = UserProfile.builder().firstName("Bob").lastName("Tran").email("b@ex.com").build();
        User u2 = User.builder().id("u2").username("bob").passwordHash("x").profile(p2).build();
        p2.setUser(u2);

        User u3 = User.builder().id("u3").username("nop").passwordHash("x").profile(null).build();

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));
        when(userProfileMapper.toDto(u1)).thenReturn(UserProfileResponse.builder().username("alice").build());

        List<UserProfileResponse> result = service.getUserProfileByName("  nGuYeN ");

        assertThat(result).extracting(UserProfileResponse::getUsername).containsExactly("alice");
        verify(userProfileMapper).toDto(u1);
        verify(userProfileMapper, never()).toDto(u2);
    }
}
