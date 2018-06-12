//此JS版本适用于mysql--> limit offset 分页
function PaginationLichenyi(page, size, offset, totalPage, totalCount){
	this.size = size || 10;				//默认页大小
	this.offset = offset || 1;			//默认当前第几条
	this.page = page || 1;				//默认当前页
	this.totalCount = totalCount || 0;	//默认总数量
	this.totalPage = totalPage || 0;	//默认总共多少页
	
	this.showCount = 5;					//页面上显示多少页（也就是多少个数字， 如果值为5表示算上显示当前页前后共5个数字）
	this.requestUrl = 'manager/article/GetArticleList';
	
	this.init();
} 
PaginationLichenyi.prototype={
	init:function(){//初始化
		var _this = this;
		//将页码循环到页面
		//得到即将循环的开始页和结束页的页码
		var startPage;
		var endPage;
		//当前页是首页的情况 或者 如果当前页在1~3（假设页面显示showCount=5的情况下）
		if(_this.page == 1 || _this.page < parseInt(_this.showCount/2)){
			startPage = 1;
			endPage = (_this.showCount > _this.totalPage) ? _this.totalPage : _this.showCount;
		}else if(_this.page == _this.totalPage || (_this.totalPage-_this.page) < parseInt(_this.showCount/2)){//如果当前页在3~5（假设页面显示showCount=5的情况下）
			startPage = _this.totalPage - _this.showCount;
			startPage = startPage < 1 ? 1 : startPage;
			endPage = _this.totalPage;
		}else{
			startPage = _this.page - parseInt(_this.showCount/2);
			endPage = _this.page + parseInt(_this.showCount/2);
		}
		$('.am-pagination,.am-pagination').html('');//清空底部分页栏
		$('.am-pagination,.am-pagination').append('<li ><a href="javascript:void(0);" onclick="pagination.prePage(this);">&lt;&lt;前一页</a></li>');
		for(;startPage <= endPage;startPage++){
			var active = '';
			if(_this.page == startPage)
				active = 'class="am-active"';
			$('.am-pagination,.am-pagination').append('<li '+active+'><a href="javascript:void(0);" onclick="pagination.skipPage(this, '+startPage+')">'+startPage+'</a></li>');
		}
		$('.am-pagination,.am-pagination').append('<li ><a href="javascript:void(0);" onclick="pagination.nextPage(this);">后一页&gt;&gt;</a></li>');
	},
	prePage:function(obj){//前一页
		var _this = this;
		console.log("前一页  "+_this.page);
		var previousPage = _this.page - 1;
		_this.page = (previousPage < 1) ? 1 : previousPage;
		var offsett = (_this.page-1) * _this.size + 1;
		_this.offset = (offsett > _this.totalCount)?((_this.totalPage-1) * _this.size + 1):(offsett);
		_this.hrefPage(obj);
	},
	nextPage:function(obj){//后一页
		var _this = this;
		console.log("后一页  "+_this.page);
		var nexPage = _this.page + 1;
		_this.page = (nexPage > _this.totalPage) ? _this.totalPage : nexPage;
		var offsett = (_this.page) * _this.size + 1;
		_this.offset = (offsett > _this.totalCount)?((_this.totalPage-1) * _this.size + 1):(offsett);
		_this.hrefPage(obj);
	},
	homePage:function(obj){//首页
		var _this = this;
		_this.offset = 1;
		_this.page = 1;
		_this.hrefPage(obj);
	},
	endPage:function(obj){//尾页
		var _this = this;
		_this.page = _this.totalPage;
		_this.offset = (_this.totalPage-1) * _this.size + 1;
		_this.hrefPage(obj);
	},
	skipPage:function(obj, page){//跳转页码
		var _this = this;
		_this.page = page;
		_this.offset = (page-1) * _this.size + 1;
		_this.hrefPage(obj);
	},
	hrefPage:function(obj){
		var _this = this;
		//链接跳转分页
		//$(obj).attr("href", _this.rquestUrl+"?offset="+_this.offset+"&size="+_this.size+"&titleContain="+_this.titleContain+"&tag="+_this.tag);
		//ajax异步分页
		_this.ajaxPage();
	}, 
	ajaxPage:function(){
		var _this = this;
		var data = {"offset":_this.offset, "size":_this.size, "titleContain":$('#tagSelect').val(), "tag":$('#titleContain').val(), "ajax":"YES"};
		_this.requestRemoteData(data, function(result){
			console.log(JSON.stringify(result));
			var result_offset = parseInt(result.offset);
			var html = '';
			var list = result.list;
			for(var i= 0;i < list.length; i++){
				var listData = list[i];
				var tagData = listData.tags;
				var tag = '';
				for(var j = 0;j < tagData.length; j++){
					if(tag != '')
						tag += ','
					tag += tagData[j];
				}
				html += '<tr>'+
							'<td>'+(result_offset+i)+'</td>'+
							'<td>'+
							'	<a href="manager/article/content?id='+listData.id+'">'+listData.title+'</a>'+
							'</td>'+
							'<td>'+tag+'</td>'+
							'<td class="am-hide-sm-only">'+listData.author+'</td>'+
							'<td class="am-hide-sm-only">'+listData.createTimeStr+'</td>'+
							'<td>'+
							'	<div class="am-btn-toolbar">'+
							'		<div class="am-btn-group am-btn-group-xs">'+
							'			<a href="manager/article/content?id='+listData.id+'" class="am-btn am-btn-default am-btn-xs am-text-secondary"><span class="am-icon-pencil-square-o"></span> 编辑</a>'+
							'			<a href="manager/article/remove?id='+listData.id+'" class="am-btn am-btn-default am-btn-xs am-text-danger am-hide-sm-only"><span class="am-icon-trash-o"></span> 删除</a>'+
							'		</div>'+
							'	</div>'+
							'</td>'+
						'</tr>';
			}
			$('tbody').html(html);
			
			//重新初始化底部分页
			_this.size = _this.parseResult(result, "size");				//默认页大小
			_this.offset = _this.parseResult(result, "offset");			//默认当前第几条
			_this.page = _this.parseResult(result, "page");				//默认当前页
			_this.totalCount = _this.parseResult(result, "totalCount");	//默认总数量
			_this.totalPage = _this.parseResult(result, "totalPage");	//默认总共多少页
			_this.init();
		});
	},
	requestRemoteData:function(data, callback){
		var _this = this;
		console.log(JSON.stringify(data));
		$.ajax({
            url: _this.requestUrl,
            type: 'GET',
            async: true,
            dataType: "json",
            data: data,
            success: callback,
            beforeSend:function(){
			},
			complete:function(){
            },
            error: function (data) {
            	console.log(_this.requestUrl+"请求数据失败");
            }
        });
	},
	parseResult:function(){
		var size = arguments.length;
		if(size == 0){
			return '';
		}
		var result = arguments[0];
		for(var i = 1; i < size; i++){
			if(result == undefined){
				return '';
			}
			var argu = arguments[i];
			if(i == size){
				return argu;
			}
			result = result[argu];
		}
		return result;
	}
}