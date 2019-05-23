let stompClient = null;
let me = null;
let openChat = null;
let users = null;
let chats = [];

const FROM_SYSTEM = "BigBrotherSystem";
const TYPE_LOGIN_ACK = "login_ack";
const TYPE_TEXT = "text";
const TYPE_FILE = "file";
const TYPE_REQUEST_MESSAGES = "request_messages";
const TYPE_CREATE_ROOM = "create_room";
const TYPE_DROP_ROOM = "drop_room";
const TYPE_USERS = "users";
const TYPE_SYSTEM_MESSAGE = "system_message";
const SUPER = "superUser";
const NORMAL = "normalUser";
const GROUP_SYSTEM = "system";
const GROUP_ADMIN = "admin";
const TYPE_INVITE_USER = "invite_user";
const TYPE_NOTIFICATION = "notification";
const TYPE_ADMIN = "admin";


function connect(user) {
    const socket = new SockJS('/ws-brother');
    stompClient = Stomp.over(socket);
    stompClient.connect({"user": user}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/queue/reply', function (message) {
            showMessage(JSON.parse(message.body));
        });
        stompClient.send("/app/send", {}, JSON.stringify({'type': 'login', 'content': user}));
    });
}

function showMessage(message) {

    console.log(message);

    switch (message.type) {
        case TYPE_NOTIFICATION:
        case TYPE_FILE:
        case TYPE_TEXT:
            spawnNotification(message);
            if (message.to === openChat)
                addChatMessage(message);
            break;

        case TYPE_LOGIN_ACK:
            me = message.content;
            appendUserOptions(me.type);
            rooms = me.rooms;
            loadRooms(rooms);
            break;

        case TYPE_REQUEST_MESSAGES:
            const messages = message.content;
            messages.forEach(function (m) {
                addChatMessage(m);
            });
            break;

        case TYPE_USERS:
            users = message.content;
            loadUsers(users);
            break;

        case TYPE_CREATE_ROOM:
            appendRoom(message.content);
            break;

        case TYPE_DROP_ROOM:
            dropRoom(message.content);
            break;
    }
}

function spawnNotification(message) {
    if (message.to === openChat) return;
    let badge = $('#badge-' + message.to).text();
    if (badge === '') {
        $('#badge-' + message.to).text("1");
    } else {
        let count = parseInt(badge, 10);
        count++;
        $('#badge-' + message.to).text(count);
    }
}

function appendUserOptions(typeUser) {
    switch (typeUser) {
        case SUPER:
            $('#options_menu').append('<li id="btn_sys_msg"><i class="fas fa-broadcast-tower"></i>System message</li>');
            $('#btn_sys_msg').click(function () {
                $('#modalSysMsg').modal('toggle');
            });
            break;
    }
}

function addChatMessage(message) {
    const date = moment(message.timestamp).fromNow();
    switch (message.type) {
        case TYPE_TEXT:

            if (message.from === me.id) {
                // language=HTML
                $("#chat_body").append('<div class=\"d-flex justify-content-end mb-4\">'
                    + '<div class="msg_cotainer_send">'
                    + message.content
                    + '<span class="msg_time">' + date + '</span>'
                    + '</div>'
                    + '<div class="img_cont_msg">'
                    + '<img class="rounded-circle user_img_msg" src="" >'
                    + '</div>'
                    + '</div>');
            } else {
                // language=HTML
                $("#chat_body").append('<div class=\"d-flex justify-content-start mb-4\">'
                    + '<div class="img_cont_msg">'
                    + '<img class="rounded-circle user_img_msg" src="" >'
                    + '</div>'
                    + '<div class="msg_cotainer">'
                    + message.content
                    + '<span class="msg_time">' + date + '</span>'
                    + '</div>'
                    + '</div>');
            }
            break;

        case TYPE_FILE:
            if (message.content.fileType === "image") {
                if (message.from === me.id) {
                    // language=HTML
                    $("#chat_body").append('<div class=\"d-flex justify-content-end mb-4\">'
                        + '<div class="msg_cotainer_send">'
                        + '<img src="' + message.content.fileDownloadUri + '" >'
                        + '<span class="msg_time">' + date + '</span>'
                        + '</div>'
                        + '<div class="img_cont_msg">'
                        + '<img class="rounded-circle user_img_msg" src="" >'
                        + '</div>'
                        + '</div>');
                } else {
                    // language=HTML
                    $("#chat_body").append('<div class=\"d-flex justify-content-start mb-4\">'
                        + '<div class="img_cont_msg">'
                        + '<img class="rounded-circle user_img_msg" src="" >'
                        + '</div>'
                        + '<div class="msg_cotainer">'
                        + '<img src="' + message.content.fileDownloadUri + '" >'
                        + '<span class="msg_time">' + date + '</span>'
                        + '</div>'
                        + '</div>');
                }
            } else {
                if (message.from === me.id) {
                    // language=HTML
                    $("#chat_body").append('<div class=\"d-flex justify-content-end mb-4\">'
                        + '<div class="msg_cotainer_send">'
                        + '<a href="' + message.content.fileDownloadUri + '" class="btn btn-success">'
                        + '<span class="fas fa-paperclip"></span>&nbsp'
                        + message.content.fileName + '</a>'
                        + '<span class="msg_time">' + date + '</span>'
                        + '</div>'
                        + '<div class="img_cont_msg">'
                        + '<img class="rounded-circle user_img_msg" src="" >'
                        + '</div>'
                        + '</div>');
                } else {
                    // language=HTML
                    $("#chat_body").append('<div class=\"d-flex justify-content-start mb-4\">'
                        + '<div class="img_cont_msg">'
                        + '<img class="rounded-circle user_img_msg" src="" >'
                        + '</div>'
                        + '<div class="msg_cotainer">'
                        + '<a href="' + message.content.fileDownloadUri + '" class="btn btn-success">'
                        + '<span class="fas fa-paperclip"></span>&nbsp'
                        + message.content.fileName + '</a>'
                        + '<span class="msg_time">' + date + '</span>'
                        + '</div>'
                        + '</div>');
                }
            }

            break;

        case TYPE_NOTIFICATION:
            $("#chat_body").append('<div class=\"d-flex justify-content-start mb-4\">'
                + '<div style="background-color: #cfcfcf" class="msg_cotainer">'
                + message.content
                + '<span class="msg_time">' + date + '</span>'
                + '</div>'
                + '</div>');
            break;
    }
    document.getElementById('chat_body').scrollTop = 9999999;
}

function send() {
    let textInput = $("#text_message");
    let type = "";
    if (chats[openChat].typeRoom === GROUP_ADMIN) {
        type = TYPE_ADMIN;
    } else {
        type = TYPE_TEXT;
    }
    if (textInput.val() !== "") {
        stompClient.send("/app/send", {}, JSON.stringify({
            'type': type,
            'from': me.id,
            'to': openChat,
            'content': textInput.val()
        }));
        textInput.val("");
    }
}

function loadUsers(users) {
    users.forEach(function (user) {
        if (user.id !== me.id) {
            $("#usersSelect").append(new Option(user.name, user.id));
        }
    });
    $('#usersSelect').selectpicker("refresh");
}

function loadRooms(rooms) {
    let i;
    chats = [];
    for (i = 0; i < rooms.length; i++) {
        appendRoom(rooms[i]);
    }
}

function appendRoom(room) {
    chats[room.idRoom] = room;
    $("#conversations").append('<li id="' + room.idRoom + '">'
        + '<div class="d-flex bd-highlight">'
        + '<div class="img_cont">'
        + '<img class="rounded-circle user_img">'
        + '<span class="online_icon"></span>'
        + '</div>'
        + '<div class="user_info">'
        + '<span>' + getNameFromRoom(room) + ' <span id="badge-' + room.idRoom + '" class="badge badge-pill badge-success"></span></span>'
        + '<p>' + getNameFromRoom(room) + ' is online</p>'
        + '</div>'
        + '</div>'
    );

    $('#' + room.idRoom).click(function () {
        const id = $(this).attr('id').replace('#', '');
        openRoom(chats[id]);
    });
}

function dropRoom(room) {
    chats[room] = null;
    $('#' + room).remove();
    clearChat();

}

function clearChat() {
    $('#chat_body').empty();
    $('#chat_name').text("");
    $('#label_chat').text("");
}

function openRoom(room) {
    if (me.type === NORMAL && room.typeRoom === GROUP_SYSTEM) {
        $('#text_message').prop('disabled', true);
        $("#send_btn").css("pointer-events", "none");
        $("#file_button").css("pointer-events", "none");
        $('#chat_controls').hide();
        $('#btn_add_user').hide();
    } else {
        $('#text_message').prop('disabled', false);
        $("#send_btn").css("pointer-events", "auto");
        $("#file_button").css("pointer-events", "auto");
        $('#chat_controls').show();

        if (room.creatorRoom === me.id && room.group) {
            $('#btn_add_user').show();
            $('#btn_remove_room').show();
        } else {
            $('#btn_add_user').hide();
            $('#btn_remove_room').hide();
        }
    }
    $('#chat_body').empty();
    $('#' + room.idRoom).addClass("active");
    $('#' + openChat).removeClass("active");
    $('#chat_name').text(getNameFromRoom(room));
    $('#label_chat').text(room.membersRoom.join(', '));
    $('#badge-' + room.idRoom).text('');

    $('#addUserSelect').empty();
    users.forEach(function (user) {
        if ($.inArray(user.name, room.membersRoom) === -1) {
            $("#addUserSelect").append(new Option(user.name, user.id));
        }
    });
    $('#addUserSelect').selectpicker("refresh");

    openChat = room.idRoom;
    stompClient.send("/app/send", {}, JSON.stringify({'from': me.id, 'type': 'request_messages', 'content': openChat}));
}

function getNameFromRoom(room) {
    if (room.typeRoom === "private") {
        let name = "";
        room.membersRoom.forEach(function (member) {
            if (member !== me.name) {
                name = member;
            }
        })
        return name;
    } else {
        return room.nameRoom;
    }
}

$(document).ready(function () {

    moment().format();
    $('select').selectpicker();

    // Listeners
    $('#action_menu_btn').click(function () {
        $('.action_menu').toggle();
    });

    $('#send_btn').click(function () {
        send();
    });

    $('#file_button').on('click', function () {
        $('#file_input').trigger('click');
    });

    $('#btn_new_group').on('click', function () {
        $('#modalRoom').modal('toggle');
    });

    $('#btn_add_user').click(function () {
        $('#modalInviteRoom').modal('toggle');
    });

    $('#btn_remove_room').click(function () {
        stompClient.send("/app/send", {}, JSON.stringify({
            'type': TYPE_DROP_ROOM,
            'from': me.id,
            'to': openChat,
            'content': {'Drop Room': ''}
        }));
    });

    $('#btn_inviteUsers').click(function () {

        let users = [];
        $.each($("#addUserSelect option:selected"), function () {
            users.push($(this).val());
        });

        stompClient.send("/app/send", {}, JSON.stringify({
            'type': TYPE_INVITE_USER,
            'from': me.id,
            'to': openChat,
            'content': {'membersRoom': users}
        }));

        $('#modalInviteRoom').modal('hide');
    });

    $('#btn_sendSystemMessage').on('click', function () {
        stompClient.send("/app/send", {}, JSON.stringify({
            'type': TYPE_SYSTEM_MESSAGE,
            'from': me.id,
            'to': FROM_SYSTEM,
            'content': $('#inputSystemMessage').val()
        }));
        $('#modalSysMsg').modal('hide');
    });

    $('#btn_createRoom').on('click', function () {

        let users = [];
        $.each($("#usersSelect option:selected"), function () {
            users.push($(this).val());
        });

        stompClient.send("/app/send", {}, JSON.stringify({
            'type': TYPE_CREATE_ROOM,
            'from': me.id,
            'to': 'server',
            'content': {
                'nameRoom': $('#nameGroupInput').val(),
                'membersRoom': users
            }
        }));

        $('#modalRoom').modal('hide');
    });

    $('#file_input').change(function () {
        let formdata = new FormData();
        if ($(this).prop('files').length > 0) {
            let file = $(this).prop('files')[0];
            formdata.append("file", file);
            formdata.append("from", me.id);
            formdata.append("to", openChat);
        }

        $.ajax({
            url: "/uploadFile",
            type: "POST",
            data: formdata,
            processData: false,
            contentType: false
        });
    });

    $(document).on('keypress', function (e) {
        if (e.which === 13) {
            e.preventDefault();
            send();
        }
    });

    const user = prompt("Please enter your username", "Samuel");

    if (user != null || user !== "") {
        connect(user);
    }

});