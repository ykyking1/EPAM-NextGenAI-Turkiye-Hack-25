package com.burak.openai.controller;

import com.burak.openai.model.QuizResponse;
import com.burak.openai.rag.UserDocumentRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "http://localhost:3000")
public class QuizController {
	
	private final ChatClient quizChatClient;
	private final ChatClient quizFallbackChatClient;
	private final ChatClient quizContentAnalyzerClient;
	private final ObjectMapper objectMapper;
	
	@Value("classpath:/promptTemplates/quizGenerationPromptTemplate.st")
	private Resource quizGenerationTemplate;
	
	@Value("classpath:/promptTemplates/quizFallBackPromptTemplate.st")
	private Resource quizFallbackTemplate;
	
	public QuizController(@Qualifier("quizChatClient") ChatClient quizChatClient,
	                      @Qualifier("quizFallbackChatClient") ChatClient quizFallbackChatClient,
	                      @Qualifier("quizContentAnalyzerClient") ChatClient quizContentAnalyzerClient,
	                      ObjectMapper objectMapper) {
		this.quizChatClient = quizChatClient;
		this.quizFallbackChatClient = quizFallbackChatClient;
		this.quizContentAnalyzerClient = quizContentAnalyzerClient;
		this.objectMapper = objectMapper;
	}
	
	@PostMapping("/generate-structured")
	public ResponseEntity<String> generateStructuredQuiz(@RequestBody Map<String, Object> request) {
		String username = (String) request.getOrDefault("username", "burak");
		Integer questionCount = (Integer) request.getOrDefault("questionCount", 5);
		String difficulty = (String) request.getOrDefault("difficulty", "medium");
		
		System.out.println("=== QUIZ GENERATION WITH SETTINGS ===");
		System.out.println("Username: " + username);
		System.out.println("Question Count: " + questionCount);
		System.out.println("Difficulty: " + difficulty);
		
		// Validate question count
		if (questionCount < 3 || questionCount > 10) {
			return ResponseEntity.badRequest()
				.body("{\"error\": \"Question count must be between 3 and 10\"}");
		}
		
		// Validate difficulty
		if (!difficulty.equals("easy") && !difficulty.equals("medium") && !difficulty.equals("hard")) {
			return ResponseEntity.badRequest()
				.body("{\"error\": \"Difficulty must be easy, medium, or hard\"}");
		}
		
		try {
			// Set current username for retriever
			UserDocumentRetriever.setCurrentUsername(username);
			
			// PHASE 1: Content Analysis with specialized client
			System.out.println("Phase 1: Analyzing document content...");
			String documentContent = quizContentAnalyzerClient.prompt()
				.user("Analyze the document and extract key topics, important concepts, definitions, facts, and information that can be used for creating educational quiz questions. Focus on the most significant content.")
				.call()
				.content();
			
			System.out.println("Document content length: " + documentContent.length());
			
			// If document content is not found or insufficient
			if (documentContent.length() < 100 ||
				documentContent.contains("The answer to this question") ||
				documentContent.contains("I don't know") ||
				documentContent.toLowerCase().contains("not found")) {
				
				System.out.println("ERROR: No valid document content found");
				return ResponseEntity.ok("{\"error\": \"Document content not found. Please upload a document first.\"}");
			}
			
			// PHASE 2: Quiz generation with specialized quiz client
			System.out.println("Phase 2: Generating quiz with settings...");
			
			// Token optimization - truncate content if too long
			String truncatedContent = documentContent.length() > 2000 ?
				documentContent.substring(0, 2000) + "..." : documentContent;
			
			// Load template and create prompt
			String template = quizGenerationTemplate.getContentAsString(StandardCharsets.UTF_8);
			String quizPrompt = template
				.replace("{questionCount}", String.valueOf(questionCount))
				.replace("{difficulty}", getDifficultyInTurkish(difficulty))
				.replace("{documentContent}", truncatedContent);
			
			// Quiz client with structured output
			QuizResponse quizResponse = quizChatClient.prompt()
				.user(quizPrompt)
				.call()
				.entity(QuizResponse.class);
			
			if (quizResponse.questions() != null && !quizResponse.questions().isEmpty()) {
				System.out.println("Quiz generated successfully with " + quizResponse.questions().size() + " questions");
				String jsonResponse = objectMapper.writeValueAsString(quizResponse);
				return ResponseEntity.ok(jsonResponse);
			} else {
				System.out.println("Empty quiz response, trying fallback...");
				return generateFallbackQuiz(username, questionCount, difficulty, truncatedContent);
			}
			
		} catch (Exception e) {
			System.err.println("=== QUIZ GENERATION ERROR ===");
			System.err.println("Error type: " + e.getClass().getSimpleName());
			System.err.println("Error message: " + e.getMessage());
			e.printStackTrace();
			
			// Fallback approach
			return generateEmergencyFallbackQuiz(username, questionCount);
		} finally {
			UserDocumentRetriever.clearCurrentUsername();
		}
	}
	
	/**
	 * Convert difficulty to Turkish for better prompt understanding
	 */
	private String getDifficultyInTurkish(String difficulty) {
		return switch (difficulty) {
			case "easy" -> "kolay";
			case "medium" -> "orta";
			case "hard" -> "zor";
			default -> "orta";
		};
	}
	
	/**
	 * Fallback with specialized fallback client
	 */
	private ResponseEntity<String> generateFallbackQuiz(String username, int questionCount, String difficulty, String content) {
		try {
			System.out.println("=== FALLBACK QUIZ GENERATION ===");
			UserDocumentRetriever.setCurrentUsername(username);
			
			// Load fallback template
			String template = quizFallbackTemplate.getContentAsString(StandardCharsets.UTF_8);
			String fallbackPrompt = template
				.replace("{questionCount}", String.valueOf(questionCount))
				.replace("{difficulty}", getDifficultyInTurkish(difficulty))
				.replace("{documentContent}", content.length() > 800 ? content.substring(0, 800) : content);
			
			String rawResponse = quizFallbackChatClient.prompt()
				.user(fallbackPrompt)
				.call()
				.content();
			
			// Extract and validate JSON
			String jsonPart = extractJsonFromResponse(rawResponse);
			QuizResponse testParse = objectMapper.readValue(jsonPart, QuizResponse.class);
			
			System.out.println("Fallback quiz generated successfully");
			return ResponseEntity.ok(objectMapper.writeValueAsString(testParse));
			
		} catch (Exception fallbackEx) {
			System.err.println("Fallback approach failed: " + fallbackEx.getMessage());
			return generateEmergencyFallbackQuiz(username, questionCount);
		} finally {
			UserDocumentRetriever.clearCurrentUsername();
		}
	}
	
	/**
	 * Emergency fallback - hardcoded quiz when everything fails
	 */
	private ResponseEntity<String> generateEmergencyFallbackQuiz(String username, int questionCount) {
		System.out.println("=== EMERGENCY FALLBACK QUIZ ===");
		
		StringBuilder questions = new StringBuilder();
		for (int i = 1; i <= Math.min(questionCount, 3); i++) {
			if (i > 1) questions.append(",");
			questions.append(String.format("""
				{
				  "question": "Document analysis question %d (Technical issue occurred)",
				  "options": {
				    "A": "Primary concept from the document",
				    "B": "Secondary concept from the document",
				    "C": "Alternative concept from the document",
				    "D": "Additional concept from the document"
				  },
				  "answer": "A"
				}
				""", i));
		}
		
		String emergencyQuiz = String.format("""
			{
			  "questions": [%s],
			  "note": "Quiz created with limited functionality due to technical issue. Please try again."
			}
			""", questions.toString());
		
		return ResponseEntity.ok(emergencyQuiz);
	}
	
	/**
	 * Extract JSON from response string
	 */
	private String extractJsonFromResponse(String response) {
		if (response == null) return "{}";
		
		int start = response.indexOf("{");
		int end = response.lastIndexOf("}");
		
		if (start != -1 && end != -1 && end > start) {
			return response.substring(start, end + 1);
		}
		
		return "{}";
	}
	
	// Legacy endpoints for backward compatibility
	@PostMapping("/generate-rag")
	public ResponseEntity<String> generateQuizWithRAG(@RequestBody Map<String, Object> request) {
		return generateStructuredQuiz(request);
	}
	
	@PostMapping("/generate")
	public ResponseEntity<String> generateQuiz(@RequestBody Map<String, Object> request) {
		return generateStructuredQuiz(request);
	}
}