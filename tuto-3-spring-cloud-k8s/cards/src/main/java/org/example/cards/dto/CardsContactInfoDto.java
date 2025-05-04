package org.example.cards.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "cards")
public class CardsContactInfoDto {
    private String message;
    private Map<String, String> contactDetails;
    private List<String> onCallSupport;

//    public CardsContactInfoDto(String message, Map<String, String> contactDetails, List<String> onCallSupport) {
//        this.message = message;
//        this.contactDetails = contactDetails;
//        this.onCallSupport = onCallSupport;
//    }

}
