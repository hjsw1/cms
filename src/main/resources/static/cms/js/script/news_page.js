var newsList = '';

function doNewsAjax(pageNum,pageSize) {
    $.ajax({
        url:'/api/findNewsList',
        method: 'get',
        data: {
            pageNum: pageNum,
            pageSize: pageSize
        },
        success: function (data) {
            // data = data.reverse();
            for(let i = 0;i<data.length;i++){

            }

            debugger;
        },
        error: function (a, b, c) {

        }
    })
}