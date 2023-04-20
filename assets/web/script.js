$(document).ready(function () {
    initUser()
})

function initUser() {
    $.ajax({
        type: "GET",
        url: "/users",
        dataType: "json",

        success: function (res) {
            const fragment = $(document.createDocumentFragment())
            $.each(res, (index, user) => {
                let [input, label] = createUserItem(user)
                fragment.append(input)
                fragment.append(label)
                $("#user-list").append(fragment)
            })
        },
        error: function (err) {
            alert("Initialize user error!");
            console.log(err);
        },
    })
}

function updateData(uuid) {
    $.ajax({
        type: "POST",
        url: "/files",
        contentType: 'application/json',
        data: JSON.stringify({
            "uuid": uuid
        }),
        success: function (res) {
            $("#data-list").empty()
            $.each(res, (index, file) => {
                const fragment = $(document.createDocumentFragment())
                fragment.append(createDataItem(index, uuid, file))
                $("#data-list").append(fragment)
            })
        },
        error: function (err) {
            alert("Update files failed!");
            console.log(err);
        }
    })
}

function createUserItem(user) {
    let input = $("<input>", {
        "class": "btn-check",
        "id": `${user.id}`,
        "type": "radio",
        "name": "options",
        "autocomplete": "off",
    })
    let label = $("<label>", {
        "class": "btn btn-primary m-2",
        "for": `${user.id}`,
        "data-bs-dismiss": "modal",
        "text": `${user.id} ${user.name}`
    }).click(() => {
        $("#selected-user").text(`${user.id} ${user.name}`)
        updateData(user.uuid)
        $("#reload-data").unbind("click")
        $("#reload-data").click(() => updateData(user.uuid))
    })
    return [input, label]
}

function createDataItem(index, uuid, data) {
    let li_class = ""
    if (index % 2 == 0) li_class = "list-group-item bg-light"
    else li_class = "list-group-item"

    let li = $("<li>", { "class": li_class })

    let row = $("<div>", { "class": "row" }).appendTo(li)
    let [date, time] = dateConvert(data.date)
    let div_date = $("<div>", {
        "class": "col-2 d-flex align-items-center",
        "text": `${date}`
    }).appendTo(row)
    let div_time = $("<div>", {
        "class": "col-2 d-flex align-items-center",
        "text": `${time}`
    }).appendTo(row)
    let div_type = $("<div>", {
        "class": "col d-flex align-items-center",
        "text": `${data.type}`
    }).appendTo(row)
    let download = $("<button>", {
        "class": "btn btn-secondary col-2",
    }).appendTo(row)
    let icon = $("<i>", { "class": "bi bi-download" }).appendTo(download)
    download.append(" Download")
    download.click(() => {
        downloadFile(uuid, data.fileName, nickFileName(data))
    })
    return li;
}

function downloadFile(uuid, fileName, nickName) {
    $.ajax({
        url: "/download",
        type: 'POST',
        xhrFields: {
            responseType: 'blob'
        },
        data: JSON.stringify({
            "uuid": uuid,
            "fileName": fileName
        }),
        success: function (response) {
            // 將響應作為檔案下載
            let filename = nickName; // 下載的檔案名稱
            let url = window.URL.createObjectURL(response);

            let a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        },
        error: function (xhr, textStatus, errorThrown) {
            console.error(errorThrown);
        }
    });
}

function dateConvert(date) {
    return [date.slice(0, 4) + "-" + date.slice(4, 6) + "-" + date.slice(6, 8), date.slice(8, 10) + ":" + date.slice(10, 12)]
}

function nickFileName(data) {
    return $("#selected-user").text() + " " + data.date + " " + data.type + "." + data.fileName.split(".")[1]
}