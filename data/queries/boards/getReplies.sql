SELECT posts.postid as "messageuri", posts.userid as "contributor", posts.posteddate as "created" FROM posts WHERE posts.isop=0 AND posts.postid IN (SELECT replyingpostid FROM replies)
