pragma solidity ^0.5.10;

contract Comments {
    struct Post {
        string name;
        string comment;
        string date;
        string emoji;
        bool show;
    }
    Post[] public posts;
    
    function getPostCount() public view returns(uint) {
        return posts.length;
    }
    
    function post(string memory _name, string memory _comment, string memory _date, string memory _emoji) public {
        posts.length++;
        posts[posts.length-1].name = _name;
        posts[posts.length-1].comment = _comment;
        posts[posts.length-1].date = _date;
        posts[posts.length-1].emoji = _emoji;
        posts[posts.length-1].show = true;
    }
    
    function hidePost(uint index) public {
        posts[index].show = false;
    }

    function getPost(uint index) public view returns(string memory, string memory, string memory, string memory) {
        return (posts[index].name, posts[index].comment, posts[index].date, posts[index].emoji);
    }
    
}
