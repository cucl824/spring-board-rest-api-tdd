package com.back.domain.post.post.service;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.postComment.entity.PostComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public long count() {
        return postRepository.count();
    }

    public Post write(String title, String content) {
        return postRepository.save(new Post(title, content));
    }

    public PostComment writeComment(Post post, String content) {
        return post.addComment(content);
    }

    public List<Post> findAll() {
        return postRepository.findAllByOrderByIdDesc();
    }

    public Post findById(long id) {
        return postRepository.findById(id).orElseThrow();
    }

    public void flush() {
        postRepository.flush();
    }
}
