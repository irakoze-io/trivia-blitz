package dev.irakodes.triviablitz.service;

import dev.irakodes.triviablitz.model.Question;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final static Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    private List<Question> questions;
    private Map<String, Question> questionMap;

    public QuestionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadQuestions() {
        try (var is = new ClassPathResource("data/questions.json").getInputStream()) {
            //questions = objectMapper.readValue(is, objectMapper.getTypeFactory().constructCollectionType(List.class, Question.class));
            questions = objectMapper.readValue(is, new TypeReference<>() {
            });
            questionMap = questions.stream().collect(Collectors
                    .toMap(Question::id, q -> q));
            log.info("Loaded {} questions", questions.size());
        } catch (IOException e) {
            log.error("Failed to load questions", e);
        }
    }

    public List<Question> getAllQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public Optional<Question> getQuestionById(String id) {
        return Optional.ofNullable(questionMap.get(id));
    }

    public Question getRandomQuestion() {
        if (questions.isEmpty()) throw new IllegalStateException("No questions loaded");
        return questions.get(random.nextInt(questions.size()));
    }

    public List<Question> getByCategory(String category) {
        return questions.stream()
                .filter(q -> q.category().equalsIgnoreCase(category))
                .toList();
    }
}
