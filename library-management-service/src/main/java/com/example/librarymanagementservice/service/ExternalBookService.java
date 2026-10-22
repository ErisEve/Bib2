package com.example.librarymanagementservice.service;

import com.example.librarymanagementservice.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExternalBookService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalBookService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Book fetchFromOpenLibrary(String isbn) {
        // Clean the ISBN (remove hyphens and spaces)
        String cleanIsbn = isbn.replaceAll("\\D", "").trim();
        logger.info("Fetching book with ISBN: {}", cleanIsbn);

        // Open Library API endpoint (no API key required!)
        String url = UriComponentsBuilder.fromHttpUrl("https://openlibrary.org/api/volumes/brief/isbn/" + cleanIsbn + ".json")
                .toUriString();

        logger.info("Calling URL: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Response received");

            JsonNode root = objectMapper.readTree(response);

            // Navigate to the records
            JsonNode records = root.path("records");
            if (records.isMissingNode() || records.isEmpty()) {
                throw new RuntimeException("No book found for ISBN: " + isbn);
            }

            // Get the first record (the key is dynamic, like "/books/OL52739325M")
            String firstKey = records.fieldNames().next();
            JsonNode record = records.path(firstKey);

            // Navigate to the data object
            JsonNode data = record.path("data");
            if (data.isMissingNode()) {
                throw new RuntimeException("No data found for ISBN: " + isbn);
            }

            // Extract title
            String title = data.path("title").asText("N/A");
            logger.info("Title: {}", title);

            // Extract authors
            StringBuilder authorBuilder = new StringBuilder();
            JsonNode authorsNode = data.path("authors");
            if (authorsNode.isArray()) {
                for (int i = 0; i < authorsNode.size(); i++) {
                    authorBuilder.append(authorsNode.get(i).path("name").asText("Unknown"));
                    if (i < authorsNode.size() - 1) authorBuilder.append(", ");
                }
            }
            String authors = authorBuilder.toString();
            logger.info("Authors: {}", authors);

            // Extract description - using excerpts if available
            String description = extractDescription(record);
            logger.info("Description: {}", description);

            // Extract published year
            int publishedYear = extractPublishedYear(record);
            logger.info("Published Year: {}", publishedYear);

            String coverUrl = extractCoverUrl(record);
            logger.info("Cover URL: {}", coverUrl);

            // Build and return the Book object
            return Book.builder()
                    .title(title)
                    .author(authors.isEmpty() ? "Unknown Author" : authors)
                    .description(description)
                    .publishedYear(publishedYear)
                    .isbn(cleanIsbn)
                    .coverUrl(coverUrl)
                    .available(true)
                    .build();

        } catch (Exception e) {
            logger.error("Error fetching book: ", e);
            throw new RuntimeException("Failed to fetch book from Open Library API: " + e.getMessage(), e);
        }
    }
    /**
     * Extracts cover URL from Open Library response
     * Tries multiple locations in order of preference
     */
    private String extractCoverUrl(JsonNode record) {
        // 1. Try data.cover.medium first
        JsonNode data = record.path("data");
        JsonNode cover = data.path("cover");
        if (!cover.isMissingNode()) {
            String medium = cover.path("medium").asText();
            if (!medium.isEmpty()) {
                return medium;
            }
            String small = cover.path("small").asText();
            if (!small.isEmpty()) {
                return small;
            }
            String large = cover.path("large").asText();
            if (!large.isEmpty()) {
                return large;
            }
        }

        // 2. Try details.thumbnail_url
        JsonNode details = record.path("details");
        String thumbnailUrl = details.path("thumbnail_url").asText();
        if (!thumbnailUrl.isEmpty()) {
            return thumbnailUrl;
        }

        // 3. Try items array (first item's cover)
        JsonNode items = record.path("items");
        if (items.isArray() && items.size() > 0) {
            JsonNode firstItem = items.get(0);
            JsonNode itemCover = firstItem.path("cover");
            if (!itemCover.isMissingNode()) {
                String medium = itemCover.path("medium").asText();
                if (!medium.isEmpty()) {
                    return medium;
                }
                String small = itemCover.path("small").asText();
                if (!small.isEmpty()) {
                    return small;
                }
            }
        }

        // 4. Fallback - construct from OLID
        String olid = data.path("key").asText();
        if (!olid.isEmpty() && olid.startsWith("/books/")) {
            String cleanOlid = olid.replace("/books/", "");
            return "https://covers.openlibrary.org/b/olid/" + cleanOlid + "-M.jpg";
        }

        // 5. No cover found
        return null;
    }
    /**
     * Extracts description from Open Library response
     * Tries to get it from excerpts first, then from data.description
     */
    private String extractDescription(JsonNode record) {
        JsonNode data = record.path("data");

        // First, try to get description from excerpts
        JsonNode excerpts = data.path("excerpts");
        if (excerpts.isArray() && excerpts.size() > 0) {
            String excerpt = excerpts.get(0).path("text").asText();
            if (!excerpt.isEmpty()) {
                return excerpt;
            }
        }

        // If no excerpts, try description from data
        JsonNode description = data.path("description");
        if (!description.isMissingNode()) {
            if (description.isTextual()) {
                return description.asText();
            } else if (description.isObject()) {
                String value = description.path("value").asText();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }

        // If no description found, use a default
        return "No description available for this book.";
    }

    /**
     * Extracts published year from Open Library response
     * Tries multiple locations in the response
     */
    private int extractPublishedYear(JsonNode record) {
        JsonNode data = record.path("data");

        // Try to get publish_date from data
        JsonNode publishDate = data.path("publish_date");
        if (!publishDate.isMissingNode()) {
            String date = publishDate.asText();
            int year = extractYearFromDate(date);
            if (year > 0) {
                return year;
            }
        }

        // Try to get it from details
        JsonNode details = record.path("details");
        if (!details.isMissingNode()) {
            JsonNode detailsPublishDate = details.path("publish_date");
            if (!detailsPublishDate.isMissingNode()) {
                String date = detailsPublishDate.asText();
                int year = extractYearFromDate(date);
                if (year > 0) {
                    return year;
                }
            }
        }

        // Try to get it from items array (first item)
        JsonNode items = record.path("items");
        if (items.isArray() && items.size() > 0) {
            JsonNode firstItem = items.get(0);
            JsonNode itemPublishDate = firstItem.path("publishDate");
            if (!itemPublishDate.isMissingNode()) {
                String date = itemPublishDate.asText();
                int year = extractYearFromDate(date);
                if (year > 0) {
                    return year;
                }
            }
        }

        return 0;
    }

    /**
     * Helper method to extract a 4-digit year from a date string
     */
    private int extractYearFromDate(String date) {
        if (date == null || date.isEmpty()) {
            return 0;
        }

        Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
        Matcher matcher = pattern.matcher(date);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {}
        }

        return 0;
    }
}