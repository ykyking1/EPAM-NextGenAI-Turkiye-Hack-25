//package com.burak.openai.config;
//
//import com.burak.openai.advisor.TokenUsageAuditAdvisor;
//import com.burak.openai.rag.PIIMaskingDocumentPostProcessor;
//import com.burak.openai.tools.TimeTools;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.client.advisor.api.Advisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.MessageWindowChatMemory;
//import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
//import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
//import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//import com.burak.openai.advisor.TokenUsageAuditAdvisor;
//import com.burak.openai.rag.PIIMaskingDocumentPostProcessor;
//import com.burak.openai.tools.TimeTools;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.client.advisor.api.Advisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.MessageWindowChatMemory;
//import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
//import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
//import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//
//
//@Configuration
//public class TimeChatClientConfig {
//
//	@Bean("timeChatClient")
//	public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
//	                             ChatMemory chatMemory, TimeTools timeTools) {
//		Advisor loggerAdvisor = new SimpleLoggerAdvisor();
//		Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
//		Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
//		return chatClientBuilder
//			.defaultTools(timeTools)
//			.defaultAdvisors(List.of(loggerAdvisor, memoryAdvisor,tokenUsageAdvisor))
//			.build();
//	}
//}