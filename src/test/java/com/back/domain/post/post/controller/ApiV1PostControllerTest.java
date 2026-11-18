package com.back.domain.post.post.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1PostControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("글 작성")
    void write() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        Post post = postRepository.findAll().getLast();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".formatted(post.getId())))
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.createDate").value(startsWith(post.getCreateDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.data.modifyDate").value(startsWith(post.getModifyDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"));

        assertThat(postRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("글 작성, 빈 제목이면 400")
    void writeWithoutTitle() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("title-NotBlank")));

        assertThat(postRepository.count()).isZero();
    }


    @Test
    @DisplayName("글 다건 조회, 개수와 주요 필드 확인(최신순)")
    void list() throws Exception {
        Post post1 = postService.write("제목 1", "내용 1");
        Post post2 = postService.write("제목 2", "내용 2");
        Post post3 = postService.write("제목 3", "내용 3");
        postService.flush();

        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts"))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        Post[] expected = {post3, post2, post1};
        for (int i = 0; i < expected.length; i++) {
            Post post = expected[i];
            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(post.getId()))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(startsWith(post.getCreateDate().toString().substring(0, 19))))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(startsWith(post.getModifyDate().toString().substring(0, 19))))
                    .andExpect(jsonPath("$[%d].title".formatted(i)).value(post.getTitle()))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value(post.getContent()));
        }
    }

    @Test
    @DisplayName("글 단건 조회")
    void item() throws Exception {
        Post post = postService.write("제목 1", "내용 1");
        postService.flush();

        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d".formatted(post.getId())))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.createDate").value(startsWith(post.getCreateDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.modifyDate").value(startsWith(post.getModifyDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.title").value("제목 1"))
                .andExpect(jsonPath("$.content").value("내용 1"))
                .andExpect(jsonPath("$.resultCode").doesNotExist());
    }

    @Test
    @DisplayName("글 단건 조회, 없는 글이면 404")
    void itemNotFound() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d".formatted(Integer.MAX_VALUE)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("글 수정")
    void modify() throws Exception {
        Post post = postService.write("제목 1", "내용 1");
        postService.flush();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d".formatted(post.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "수정된 제목",
                                            "content": "수정된 내용"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 수정되었습니다.".formatted(post.getId())));

        Post modified = postService.findById(post.getId());
        assertThat(modified.getTitle()).isEqualTo("수정된 제목");
        assertThat(modified.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("글 수정, 빈 내용이면 400")
    void modifyWithoutContent() throws Exception {
        Post post = postService.write("제목 1", "내용 1");
        postService.flush();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d".formatted(post.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "수정된 제목",
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("content-NotBlank")));

        assertThat(postService.findById(post.getId()).getContent()).isEqualTo("내용 1");
    }

    @Test
    @DisplayName("글 수정, 없는 글이면 404")
    void modifyNotFound() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d".formatted(Integer.MAX_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "수정된 제목",
                                            "content": "수정된 내용"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
