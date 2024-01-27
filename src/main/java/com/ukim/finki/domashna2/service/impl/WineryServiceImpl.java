package com.ukim.finki.domashna2.service.impl;

import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResult;
import com.ukim.finki.domashna2.model.WineryInfo;
import com.ukim.finki.domashna2.model.WineryReview;
import com.ukim.finki.domashna2.model.WineryUserReview;
import com.ukim.finki.domashna2.repository.WineryRepository;
import com.ukim.finki.domashna2.repository.WineryUserReviewRepository;
import com.ukim.finki.domashna2.service.WineryService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WineryServiceImpl implements WineryService {

    @Autowired
    private WineryRepository wineryRepository;

    @Autowired
    private WineryUserReviewRepository wineryUserReviewRepository;

    @Override
    public Page<WineryInfo> getAllWineries(Pageable pageable) {
        return wineryRepository.findAll(pageable);
    }

    @Override
    public Page<WineryInfo> searchWineries(String query, Pageable pageable) {
        return wineryRepository.findByNameContainingIgnoreCase(query, pageable);
    }

    @Override
    public void saveWineryToDB(WineryInfo winery) {
        Optional<WineryInfo> existingWinery = wineryRepository.findByName(winery.getName());
        if (existingWinery.isEmpty()) {
            wineryRepository.save(winery);
        }
    }

    @Override
    public WineryInfo getWineryById(Long id) {
        return wineryRepository.findById(id).orElse(null);
    }

    @Value("https://seal-app-gfvee.ondigitalocean.app")
    private String apiBaseUrl;


    public void saveUserReviewToDB(WineryUserReview wineryUserReview) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<WineryUserReview> requestEntity = new HttpEntity<>(wineryUserReview, headers);

        String apiUrl = apiBaseUrl + "/api/wineries/" + wineryUserReview.getWineryId() + "/reviews";
        System.out.println("URL: " + apiUrl);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<WineryUserReview> responseEntity = restTemplate.postForEntity(
                apiBaseUrl + "/api/wineries/" + wineryUserReview.getWineryId() + "/reviews",
                requestEntity,
                WineryUserReview.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            WineryUserReview savedReview = responseEntity.getBody();
            wineryUserReviewRepository.save(savedReview);
         //   System.out.println(savedReview);
        } else {
            System.out.println(":(");
        }
    }

    @Override
    public List<WineryUserReview> getUserReviewsById(Long wineryId) {
        return wineryUserReviewRepository.getAllByWineryIdEquals(wineryId);
    }

    @Override
    @Transactional
    public void updateNumRatings(Long wineryId) {
        WineryInfo wineryInfo = wineryRepository.findById(wineryId).orElse(null);
        if (wineryInfo != null) {
            int totalGMRatings = wineryInfo.getReviews().size();
            int totalUserRatings = wineryUserReviewRepository.getAllByWineryIdEquals(wineryId).size();
            List<Float> userRatings = wineryUserReviewRepository.getAllRatingsByWineryId(wineryId);
            List<Float> gmRatings = wineryRepository.getRatingsByWineryId(wineryId);
            List<Float> allRatings = new ArrayList<>(userRatings);
            allRatings.addAll(gmRatings);
            double averageRating = allRatings.stream()
                    .mapToDouble(Float::doubleValue)
                    .average()
                    .orElse(0.0);
            float roundedAverageRating = (float) Math.round(averageRating * 10) / 10;
            wineryInfo.setNumRatings(totalGMRatings + totalUserRatings);
            wineryInfo.setRating(roundedAverageRating);
            wineryRepository.save(wineryInfo);
        }
    }

    @Override
    public boolean shouldIncludeWinery(PlacesSearchResult result) {
        return result.formattedAddress.contains("North Macedonia");
    }

    @Override
    public String getWebsite(PlaceDetails detailedResult) {
        if(detailedResult.website == null){
            return "Unknown";
        }
        return String.valueOf(detailedResult.website);
    }

    @Override
    public String getPhoneNumber(PlaceDetails detailedResult) {
        if(detailedResult.formattedPhoneNumber == null) return "Unknown";
        return detailedResult.formattedPhoneNumber;
    }

    @Override
    public String getOpeningTime(PlaceDetails detailedResult) {
        if (detailedResult.openingHours != null && detailedResult.openingHours.periods != null
                && detailedResult.openingHours.periods.length > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            String result = "";
            for (int i = 0; i < detailedResult.openingHours.periods.length; i++) {
                stringBuilder.append(detailedResult.openingHours.periods[i].open.day);
                stringBuilder.append(" ");
                stringBuilder.append(detailedResult.openingHours.periods[i].open.time);
                stringBuilder.append(" - ");
                stringBuilder.append(detailedResult.openingHours.periods[i].close.day);
                stringBuilder.append(" ");
                stringBuilder.append(detailedResult.openingHours.periods[i].close.time);
                stringBuilder.append("<br>");
                result = stringBuilder.toString();
            }
            return result;
        } else if (detailedResult.openingHours != null && detailedResult.openingHours.openNow) {
            return "Open 24/7";
        } else {
            return "Unknown";
        }
    }
    @Override
    public List<WineryReview> getReviews(PlaceDetails detailedResult) {
            List<WineryReview> reviews = new ArrayList<>();
            if (detailedResult.reviews != null) {
                for (com.google.maps.model.PlaceDetails.Review googleReview : detailedResult.reviews) {
                    WineryReview review = new WineryReview(
                            googleReview.authorName,
                            googleReview.language,
                            googleReview.profilePhotoUrl,
                            googleReview.relativeTimeDescription,
                            googleReview.text,
                            googleReview.time,
                            googleReview.rating
                    );
                    reviews.add(review);
                }
            }
            return reviews;
    }
}
