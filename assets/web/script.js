const DATA_TYPE = ["Patient", "CodeSystem", "DocumentReference", "Organization", "Observation", "Location"];
const DISABLE_ITEM = ["resourceType", "meta"]

// Web page initialize
$(document).ready(function() {
    addDataType();
    // data type select event
    $("#data-type-select").on("changed.bs.select", 
        function(e, clickedIndex, newValue, oldValue) {
            $("#add-option").removeAttr("disabled")
            $("#option-area").empty()
        }
    );
    // add option button click event
    $("#add-option").click(function() {
        createOption()
    })
    // reset option button click event
    $("#reset-option").click(function() {
        $("#option-area").empty()
    })
    // search button click event
    $("#search-resource").click(function() {
        searchResource(searchURL())
    });
    // previous page button click event
    $("#previous").click(function() {searchResource($(this).attr("url"))})
    // next page button click event
    $("#next").click(function() {searchResource($(this).attr("url"))})
    checkURLParams()
});

/* add data types to type select box */
function addDataType() {
    let select = $("#data-type-select");
    DATA_TYPE.forEach(function(type) {
        let option = document.createElement("option");
        option.innerHTML = type;
        select.append(option);
    });
    select.selectpicker()
}

/* use the url parameter adding specify data */
function checkURLParams() {
    let query = window.location.search
    let urlParams = new URLSearchParams(query)
    if (urlParams.has("url")) {
        let url = urlParams.get("url")
        $("#url-input").val(url)
    }
    if (urlParams.has("type")) {
        let type = urlParams.get("type")
        $("#data-type-select").selectpicker("val", type)
    }
    if (urlParams.has("id")) {
        let id = urlParams.get("id")
        createOption()
        $("#option-area select.so-select").eq(0).selectpicker("val", "id")
        $("#option-area .so-arg .opt-input").eq(0).val(id)
    }
    if (urlParams.has("url") && urlParams.has("type")) {
        searchResource(searchURL())
    }
}
/* ---------- */

// generate searching URL
function searchURL() {
    let urlInput = $("#url-input").val();
    if (urlInput == "") return alert("Please input url");
    let typeInput = $("#data-type-select").val()
    if (typeInput == undefined) return alert("Please select data type")

    let url = ""
    let search = generateSearchCondiction()
    console.log(search)
    if (search == "") url = `${urlInput}/${typeInput}`
    else url = `${urlInput}/${typeInput}?${search}`
    return url
}

// use URL to search resource
function searchResource(url) {
    if (url == "" || url == undefined) return
    $.ajax({
        type: "GET",
        url: url,
        dataType:"json",

        success:function(res) {
            $("#container").empty();
            // if not have resource
            if (res.entry == null) {
                let nothing = document.createElement("span");
                nothing.className = "h1";
                nothing.innerHTML = "Nothing is here";
                nothing.setAttribute("id", "nothing");
                $("#container").append(nothing);
                return;
            }
            // enable the switching page function if more than 20 data
            $("#next").addClass("invisible")
            $("#previous").addClass("invisible")
            $.each(res.link, function(index, obj) {
                switch (obj.relation) {
                    case "next":
                        $("#next").attr("url", obj.url)
                        $("#next").removeClass("invisible")
                        break
                    case "previous":
                        $("#previous").attr("url", obj.url)
                        $("#previous").removeClass("invisible")
                        break
                }
            })
            // display search result
            $.each(res.entry, function(index, obj) {
                setSearchResult(obj.resource);        
            });
        },
        error:function(err) {
            alert("URL not correct");
            console.log(err);
        },
    });
}

/* display search result */
function setSearchResult(res) {
    let name = changeDisplayName(res, `${res.resourceType} ${res.id}`)
    let href = res.resourceType + "-" + res.id;
    let topItem = createItem(name, href, res);
    $("#container").append(topItem.item);
}

/* change result name */
function changeDisplayName(res, name) {
    let type = $("#data-type-select").val()
    switch (type) {
        case "Patient":
            if ("name" in res) {
                if (res.name[0].text != undefined) {
                    if (isNum(res.name[0].text)) break
                    return res.name[0].text
                }
                if (res.name[0].family != undefined && res.name[0].given != undefined) {
                    let givenName = ""
                    res.name[0].given.forEach(function(value) {
                        givenName += value + " "
                    })
                    return `${givenName} ${res.name[0].family}`
                }
            }
        case "Observation":
            if ("code" in res) {
                if (res.code.text != undefined) return res.code.text
                if (res.code.coding[0].display != undefined) return res.code.coding[0].display
            }
    }
    return name
}

/* create result item */
function createItem(name, href, obj) {
    const header = document.createElement("a");
    header.innerHTML = name;
    header.className = "accordion-header button-text";
    
    const container = document.createElement("div");
    container.id = href;

    const item = document.createElement("div");
    item.className = "accordion-item";
    
    let hasChild = false;
    // create child element recursively
    for (const key in obj) {
        if (!Object.hasOwnProperty.call(obj, key)) continue;
        if (DISABLE_ITEM.includes(key)) continue;
        hasChild = true;
        const element = obj[key];
        const childName = key + ": " + element;
        if (typeof(element) == "object") {
            childItem = createItem(key, href + "-" + key, element);
        }
        else childItem = createItem(childName, href + "-" + key, {});
        container.append(childItem.item);
    }

    if (hasChild) {
        header.className += " accordion-button collapsed"
        header.href = `#${href}`;
        header.setAttribute("data-bs-toggle", "collapse");
        header.setAttribute("aria-controls", href);
        header.ariaExpanded = "false";
    }
    else header.className += " accordion-body";

    // remove item name display while an item in the array (the name is number)
    if (isNum(name)) {
        container.className = "accordion leveled show";
        item.append(container);
    }
    else {
        container.className = "accordion collapse leveled";
        item.append(header, container);
    }

    // add link to reference data
    let reg = new RegExp("reference: ([A-Za-z]*)\/([A-Za-z0-9]*)")
    if (reg.test(name)) {
        let result = name.match(reg)
        let href = location.protocol + '//' + location.host + location.pathname
        let url = $("#url-input").val()
        header.onclick = function() {
            window.open(`${href}?url=${url}&type=${result[1]}&id=${result[2]}`)
        }
    }

    return {
        item: item,
        header: header,
        container: container
    };
}
/* -- search option functions -- */

// create search option
function createOption() {
    let area = $("#option-area");
    let index = area.children().length;

    let body = document.createElement("li");
    body.className = "list-group-item search-option";

    let row = document.createElement("div");
    row.className = "row"
    
    // init search type select
    let select = document.createElement("select");
    select.className = "so-select"
    select.setAttribute("data-live-search", "true")

    // add hint option to search type select
    let defaultOption = document.createElement("option")
    defaultOption.innerHTML = "Select search type"
    defaultOption.selected = true;
    defaultOption.disabled = true;
    select.append(defaultOption)
    // add option to search type select
    let type = $("#data-type-select").val()
    for (const key in SearchOpt.OPTIONS) {
        if (Object.hasOwnProperty.call(SearchOpt.OPTIONS, key)) {
            let target = SearchOpt.OPTIONS[key].target
            if (!(target.length == 0 || target.includes(type))) continue
            let option = document.createElement("option");
            option.innerHTML = key;
            select.append(option);
        }
    }

    let arg = document.createElement("div")
    arg.className = "so-arg"

    let deleteButton = document.createElement("button");
    deleteButton.className = "btn btn-danger"
    deleteButton.type = "button"
    deleteButton.onclick = function() {
        body.remove();
    }

    let icon = document.createElement("i");
    icon.className = "bi-x-lg"
    deleteButton.append(icon)

    body.append(row);
    area.append(body);
    
    if (window.innerWidth < 768) {
        row.append(createColDiv(select, 12))
        let row2 = document.createElement("div");
        row2.className = "row pt-3"
        row2.append(createColDiv(arg, 10))
        let buttonDiv = createColDiv(deleteButton, 2)
        buttonDiv.className += " p-0"
        row2.append(buttonDiv)
        body.append(row2)
    }
    else {
        row.append(createColDiv(select, 4))
        row.append(createColDiv(arg, 7))
        let buttonDiv = createColDiv(deleteButton, 1)
        buttonDiv.className += " p-0"
        row.append(buttonDiv)
    }

    $("#option-area select.so-select").eq(index).on("changed.bs.select", 
        function(e, clickedIndex, newValue, oldValue) {
            arg.innerHTML = ""
            SearchOpt.OPTIONS[this.value].createFunc(arg)
        }
    ).selectpicker()
}

// generate search condiction
function generateSearchCondiction() {
    let selects = $("#option-area select.so-select")
    let result = ""
    for (let i = 0; i < selects.length; i++) {
        let condict = selects.eq(i).val()
        if (condict == undefined) continue
        let option = SearchOpt.OPTIONS[condict]
        let str = option.getFunc(option.searchText, $("#option-area .so-arg").eq(i))
        if (str == "") continue
        if (result != "" && !result.endsWith("&")) result += "&"
        result += str
    }
    return result
}