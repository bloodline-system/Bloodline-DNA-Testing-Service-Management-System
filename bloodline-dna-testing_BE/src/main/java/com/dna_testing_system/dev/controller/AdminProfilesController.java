package com.dna_testing_system.dev.controller;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminProfilesController {
    UserProfileService userProfileService;


    @GetMapping("/manage/profiles")
    public String getAllProfiles(Model model) {
        List<UserProfileResponse> userProfiles = userProfileService.getUserProfiles();
        model.addAttribute("userProfiles", userProfiles);
        return "admin-profiles"; // Return the view name for the admin profiles page
    }

//    @GetMapping("/manage/profile")
//    public String getProfileByUsername(@RequestParam("username") String username, Model model) {
//        UserProfileResponse userProfile = userProfileService.getUserProfile(username);
//        model.addAttribute("userProfile", userProfile);
//        return "admin-profiles"; // Return the view name for the specific profile page
//    }

    @GetMapping("/manage/search-profiles")
    public String searchProfiles(@RequestParam(value = "query",required = false) String query, Model model) {
        if(query==null || query.isEmpty()) {
            return "redirect:/manage/profiles"; // Redirect to all profiles if query is empty
        }
        UserProfileResponse userProfiles = userProfileService.getUserProfile(query);
        if(userProfiles == null) {
            return "redirect:/manage/profiles"; // Return the view name for the admin profiles page with an error message
        }
        model.addAttribute("userProfiles", userProfiles);
        return "admin-profiles"; // Return the view name for the admin profiles page
    }



    // Show update profile form by username
    @GetMapping("/manage/profile/update")
    public String showUpdateProfileForm(@RequestParam("username") String username, Model model) {
        UserProfileResponse userProfile = userProfileService.getUserProfile(username);
        model.addAttribute("userProfile", userProfile);
        return "admin-edit-profile"; // Return the view name for the update profile page
    }



    // Update profile by username
    @PostMapping("/manage/profile/update")
    public String updateProfile(@ModelAttribute("userEditProfile") UserProfileRequest userProfile,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                @RequestParam("username") String username) {
        UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
        if(file.getOriginalFilename().equals("")) {
            userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
        }
        else{
            String uploadsDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadsDir + fileName);

            try{
                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String imageUrl = "/uploads/" + fileName;
            userProfile.setProfileImageUrl(imageUrl);
        }
        if(userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }
        userProfileService.updateUserProfile(username, userProfile);
        return "redirect:/manage/profiles";
    }



    //delete profile by username
    @GetMapping("/manage/profile/delete")
    public String deleteProfile(@RequestParam("username") String username) {
        userProfileService.deleteUserProfile(username);
        return "redirect:/manage/profiles"; // Redirect to the admin profiles page after deletion
    }
}
