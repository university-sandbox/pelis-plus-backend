package com.example.template.movie.sync;

import com.example.template.config.TmdbProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(RestClient tmdbRestClient, TmdbProperties properties) {
        this.restClient = tmdbRestClient;
        this.properties = properties;
    }

    public TmdbGenreResponse getMovieGenres() {
        return restClient.get()
            .uri(uriBuilder -> {
                uriBuilder.path("/genre/movie/list")
                    .queryParam("language", properties.resolvedLanguage());
                addApiKey(uriBuilder);
                return uriBuilder.build();
            })
            .headers(headers -> {
                if (properties.hasAccessToken()) {
                    headers.setBearerAuth(properties.accessToken());
                }
            })
            .retrieve()
            .body(TmdbGenreResponse.class);
    }

    public TmdbMoviePage getMovieList(String listName, int page) {
        return restClient.get()
            .uri(uriBuilder -> {
                uriBuilder.path("/movie/{listName}")
                    .queryParam("language", properties.resolvedLanguage())
                    .queryParam("region", properties.resolvedRegion())
                    .queryParam("page", page);
                addApiKey(uriBuilder);
                return uriBuilder.build(listName);
            })
            .headers(headers -> {
                if (properties.hasAccessToken()) {
                    headers.setBearerAuth(properties.accessToken());
                }
            })
            .retrieve()
            .body(TmdbMoviePage.class);
    }

    public TmdbMovieDetails getMovieDetails(Long movieId) {
        return restClient.get()
            .uri(uriBuilder -> {
                uriBuilder.path("/movie/{movieId}")
                    .queryParam("language", properties.resolvedLanguage())
                    .queryParam("append_to_response", "videos");
                addApiKey(uriBuilder);
                return uriBuilder.build(movieId);
            })
            .headers(headers -> {
                if (properties.hasAccessToken()) {
                    headers.setBearerAuth(properties.accessToken());
                }
            })
            .retrieve()
            .body(TmdbMovieDetails.class);
    }

    private void addApiKey(org.springframework.web.util.UriBuilder uriBuilder) {
        if (!properties.hasAccessToken() && properties.hasApiKey()) {
            uriBuilder.queryParam("api_key", properties.apiKey());
        }
    }
}
