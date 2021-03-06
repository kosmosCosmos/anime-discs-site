<%@ page import="com.animediscs.model.DiscType" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <%@ include file="include/meta.jsp" %>
    <title>Anime Discs - 碟片信息</title>
    <%@ include file="include/import.jsp" %>
</head>
<body>
<%@ include file="include/navbar.jsp" %>
<div id="content"></div>
<script id="template" type="text/html">
    <ul class="nav nav-tabs">
        <li class="active"><a href="#info-tab" data-toggle="tab">基本数据</a></li>
        <li><a href="#rank-tab" data-toggle="tab">排名数据</a></li>
        <li><a href="#other-tab" data-toggle="tab">其他数据</a></li>
        <li><a href="list_rank.jsp?id={{id}}" target="_blank">全部排名</a></li>
    </ul>
    <div class="tab-content" style="padding-top: 10px">
        <div id="info-tab" class="tab-pane fade in active">
            <div class="form-group">
                <label>碟片标题</label>
                <a href="http://rankstker.net/show.cgi?n={{asin}}" target="_blank">Sakura链接</a>
                <input type="text" class="form-control" value="{{title}}">
            </div>
            <div class="form-group">
                <label>日文原名</label>
                <a href="http://www.amazon.co.jp/dp/{{asin}}" target="_blank">Amazon链接</a>
                <div class="textarea-readonly">{{japan}}</div>
            </div>
            <div class="form-group">
                <label>简短名称</label>
                <input type="text" class="form-control" value="{{sname}}">
            </div>
            <div class="form-group">
                <label>ASIN</label>
                <input type="text" class="form-control" value="{{asin}}">
            </div>
            <div class="form-group">
                <label>碟片类型</label>
                <select class="form-control" data-value="{{type}}">
                    <option value="0">CD</option>
                    <option value="1">BD</option>
                    <option value="2">DVD</option>
                    <option value="3">BD_BOX</option>
                    <option value="4">DVD_BOX</option>
                    <option value="5">OTHER</option>
                </select>
            </div>
            <div class="form-group">
                <label>限定类型</label>
                <select class="form-control" data-value="{{amzver?'1':'2'}}">
                    <option value="1">尼限定</option>
                    <option value="2">非尼限定</option>
                </select>
            </div>
        </div>
        <div id="rank-tab" class="tab-pane fade">
            <div class="form-group">
                <label>碟片标题</label>
                <input type="text" class="form-control" value="{{title}}">
            </div>
            <div class="form-group">
                <label>Amazon1</label>
                <input type="text" class="form-control" value="{{rank1 | fm_number}}位 ({{date1 | fm_timeout}} 前)">
            </div>
            <div class="form-group">
                <label>Amazon2</label>
                <input type="text" class="form-control" value="{{rank2 | fm_number}}位 ({{date2 | fm_timeout}} 前)">
            </div>
            <div class="form-group">
                <label>Amazon3</label>
                <input type="text" class="form-control" value="{{rank3 | fm_number}}位 ({{date3 | fm_timeout}} 前)">
            </div>
            <div class="form-group">
                <label>Amazon4</label>
                <input type="text" class="form-control" value="{{rank4 | fm_number}}位 ({{date4 | fm_timeout}} 前)">
            </div>
            <div class="form-group">
                <label>Amazon5</label>
                <input type="text" class="form-control" value="{{rank5 | fm_number}}位 ({{date5 | fm_timeout}} 前)">
            </div>
        </div>
        <div id="other-tab" class="tab-pane fade">
            <div class="form-group">
                <label>碟片标题</label>
                <input type="text" class="form-control" value="{{title}}">
            </div>
            <div class="form-group">
                <label>当前/前回</label>
                <input type="text" class="form-control" value="{{curk | fm_sakura}}位/{{prrk | fm_sakura}}位">
            </div>
            <div class="form-group">
                <label>累计PT</label>
                <input type="text" class="form-control" value="{{cupt | fm_number}} pt">
            </div>
            <div class="form-group">
                <label>Nico预约</label>
                <input type="text" class="form-control" value="{{cubk | fm_number}} 预约">
            </div>
            <div class="form-group">
                <label>发售日期</label>
                <input type="text" class="form-control" value="{{release | fm_date:'yyyy/MM/dd'}}">
            </div>
            <div class="form-group">
                <label>剩余天数</label>
                <input type="text" class="form-control" value="{{sday}}天">
            </div>
        </div>
    </div>
    <div class="button-group">
        <button type="button" class="btn btn-default">返回</button>
        <span id="msg"></span>
    </div>
</script>
<script id="navbar-tmpl" type="text/html">
    <li class="dropdown hidden-xs">
        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
            功能<b class="caret"></b>
        </a>
        <ul id="control" class="dropdown-menu">
            <li><a href="javascript:navbar.refresh()">刷新</a></li>
        </ul>
    </li>
</script>
<script>

    $(function () {
        render_navber_menu();
        $.getJSON("view_disc.do", {id: ${param.id}}, function (data) {
            var $content = $("#content");
            $content.html(template("template", data));
            $content.find("select").each(function () {
                $(this).val($(this).data("value"));
            });
            $content.find(".btn-default").click(function () {
                page.back();
            });
            $(".tab-content").find(":input").each(function () {
                if ($(this).attr("id") == null) {
                    $(this).prop("disabled", true);
                }
            });
        });
    });

    function render_navber_menu() {
        $("#nav-body").prepend($("#navbar-tmpl").html());
    }

</script>
</body>
</html>
