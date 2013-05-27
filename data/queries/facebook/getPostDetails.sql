SELECT
    posts.post_id as "messageuri",
    posts.user_id as "contributor",
    posts.created_time as "created"
FROM
    posts