package com.back.domain.post.postComment.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.entity.PostComment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
class ApiV1PostCommentControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PostService postService;
    @Autowired
    private EntityManager em;

    // 각 테스트마다 새로 준비하는 데이터: post1(댓글 3건), post2(댓글 2건), post3(댓글 없음)
    private Post post1, post2, post3;
    private PostComment comment1_1, comment1_2, comment1_3, comment2_1, comment2_2;

    @BeforeEach
    void setUp() {
        post1 = postService.write("제목 1", "내용 1");
        post2 = postService.write("제목 2", "내용 2");
        post3 = postService.write("제목 3", "내용 3");
        comment1_1 = postService.writeComment(post1, "댓글 1-1");
        comment1_2 = postService.writeComment(post1, "댓글 1-2");
        comment1_3 = postService.writeComment(post1, "댓글 1-3");
        comment2_1 = postService.writeComment(post2, "댓글 2-1");
        comment2_2 = postService.writeComment(post2, "댓글 2-2");
        postService.flush();
        em.clear();
    }

    private PostComment findComment(long id) {
        em.flush();
        em.clear();
        return em.find(PostComment.class, id);
    }

    @Test
    @DisplayName("댓글 작성")
    void write() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/%d/comments".formatted(post3.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "새 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        em.clear();
        Post post = postService.findById(post3.getId());
        assertThat(post.getComments()).hasSize(1);
        PostComment comment = post.getComments().getFirst();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 작성되었습니다.".formatted(comment.getId())))
                .andExpect(jsonPath("$.data.id").value(comment.getId()))
                .andExpect(jsonPath("$.data.createDate").value(startsWith(comment.getCreateDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.data.modifyDate").value(startsWith(comment.getModifyDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.data.postId").value(post3.getId()))
                .andExpect(jsonPath("$.data.content").value("새 댓글"));
    }

    @Test
    @DisplayName("댓글 작성, 빈 내용이면 400")
    void writeWithoutContent() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/%d/comments".formatted(post3.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("content-NotBlank")));

        em.clear();
        assertThat(postService.findById(post3.getId()).getComments()).isEmpty();
    }

    @Test
    @DisplayName("댓글 작성, 없는 글이면 404")
    void writeToNotFoundPost() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/%d/comments".formatted(Integer.MAX_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "새 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 다건 조회, 해당 글의 댓글만 최신순으로 반환")
    void list() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments".formatted(post1.getId())))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].postId", everyItem(is((int) post1.getId()))))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) comment2_1.getId()))))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) comment2_2.getId()))));

        PostComment[] expected = {comment1_3, comment1_2, comment1_1};
        for (int i = 0; i < expected.length; i++) {
            PostComment comment = expected[i];
            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(comment.getId()))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(startsWith(comment.getCreateDate().toString().substring(0, 19))))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(startsWith(comment.getModifyDate().toString().substring(0, 19))))
                    .andExpect(jsonPath("$[%d].postId".formatted(i)).value(post1.getId()))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value(comment.getContent()));
        }
    }

    @Test
    @DisplayName("댓글 다건 조회, 댓글이 없는 글이면 빈 배열")
    void listEmpty() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments".formatted(post3.getId())))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("댓글 다건 조회, 없는 글이면 404")
    void listNotFoundPost() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments".formatted(Integer.MAX_VALUE)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("list"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 단건 조회")
    void item() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), comment1_2.getId())))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment1_2.getId()))
                .andExpect(jsonPath("$.createDate").value(startsWith(comment1_2.getCreateDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.modifyDate").value(startsWith(comment1_2.getModifyDate().toString().substring(0, 19))))
                .andExpect(jsonPath("$.postId").value(post1.getId()))
                .andExpect(jsonPath("$.content").value("댓글 1-2"))
                .andExpect(jsonPath("$.resultCode").doesNotExist());
    }

    @Test
    @DisplayName("댓글 단건 조회, 다른 글의 댓글 ID면 404")
    void itemOfOtherPost() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), comment2_1.getId())))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 단건 조회, 없는 댓글 ID면 404")
    void itemNotFound() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), Integer.MAX_VALUE)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 수정")
    void modify() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), comment1_1.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "수정된 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 수정되었습니다.".formatted(comment1_1.getId())));

        assertThat(findComment(comment1_1.getId()).getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("댓글 수정, 빈 내용이면 400")
    void modifyWithoutContent() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), comment1_1.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": " "
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("content-NotBlank")));

        assertThat(findComment(comment1_1.getId()).getContent()).isEqualTo("댓글 1-1");
    }

    @Test
    @DisplayName("댓글 수정, 다른 글의 댓글 ID면 404이고 그 댓글은 바뀌지 않는다")
    void modifyOfOtherPost() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d/comments/%d".formatted(post1.getId(), comment2_1.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "수정된 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));

        PostComment comment = findComment(comment2_1.getId());
        assertThat(comment.getContent()).isEqualTo("댓글 2-1");
        assertThat(comment.getPost().getId()).isEqualTo(post2.getId());
    }
}
