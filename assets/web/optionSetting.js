class SearchOpt {

    static #options = {}

    /*
     * name: display name
     * searchText: search text
     * createFunc: set option display function (DOM Element) -> {}
     * getFunc: return search string function (searchText, DOM Element) -> string
     * target: what data type will use this search option
    */
    static addOption(name, searchText, createFunc, getFunc, ...target) {
        SearchOpt.#options[name] = {
            "searchText": searchText,
            "createFunc": createFunc,
            "getFunc": getFunc,
            "target": target
        }
    }

    static get OPTIONS() {
        return SearchOpt.#options
    }

}

$(document).ready(function() {
    // define the options
    SearchOpt.addOption("id", "_id", addCompareOption, getCompareString)
    SearchOpt.addOption("birthday", "birthdate", addDateOption, getDateString, "Patient")
    SearchOpt.addOption("gender", "gender", addSexOption, getSexString, "Patient")
    SearchOpt.addOption("name", "name", addCompareTextOption, getCompareTextString, "Patient")
    SearchOpt.addOption("subject", "subject", addTextOption, getTextString, "Observation")
})

// math compare type option
const compareOptions = {
    "=": "=",
    "!=": ":not=",
    ">": ":above=",
    "<": ":below="
}

function addCompareOption(optArea) {
    let grid = new GridDiv(optArea)
    let select = document.createElement("select")
    select.className = "input-group-prepend form-select opt-select"
    addSelectOption(select, compareOptions)
    
    let input = document.createElement("input")
    input.className = "form-control opt-input"
    input.setAttribute("placeholder", "value")
    
    grid.append(select, 0, 4)
    grid.append(input, 0, 8)
}

function getCompareString(text, optArea) {
    let condict = optArea.find(".opt-select").val()
    let value = optArea.find(".opt-input").val()
    if (value == "") return ""
    return text + condict + value
}

// date type option
function addDateOption(optArea) {
    let grid = new GridDiv(optArea)
    let input = document.createElement("input")
    input.className = "form-control opt-input"
    input.setAttribute("type", "text")
    input.setAttribute("placeholder", "date")

    let button = document.createElement("button");
    button.className = "input-group-addon btn btn-secondary"
    let icon = document.createElement("i")
    icon.className = "bi-calendar-check-fill icon"
    button.append(icon)
    
    let date = document.createElement("div")
    date.className = "input-group date"
    date.append(input, button)
    $(date).datetimepicker({
        useCurrent: false,
        format: "YYYY-MM-DD",
        locale: 'zh-tw',
    })
        
    grid.append(date, 0, 12)
}

function getDateString(text, optArea) {
    let value = optArea.find(".opt-input").val()
    if (value == "") return ""
    return text + "=" + value
}

// sex type option
const sexOptions = {
    "male": "male",
    "female": "female",
    "other": "other",
    "unknown": "unknown"
}

function addSexOption(optArea) {
    let grid = new GridDiv(optArea)
    let select = document.createElement("select")
    select.className = "input-group-prepend form-select opt-select"
    addSelectOption(select, sexOptions)

    grid.append(select, 0, 12)
}

function getSexString(text, optArea) {
    let value = optArea.find(".opt-select").val()
    return text + "=" + value
}

//text type option
function addTextOption(optArea) {
    let grid = new GridDiv(optArea)
    
    let input = document.createElement("input")
    input.className = "form-control opt-input"
    input.setAttribute("placeholder", "value")

    grid.append(input, 0, 8)
}

function getTextString(text, optArea) {
    let value = optArea.find(".opt-input").val()
    if (value == "") return ""
    return text + "=" + value
}

// compare text type option
const textOptions = {
    "Matches": "=",
    "Exact": ":exact="
}

function addCompareTextOption(optArea) {
    let grid = new GridDiv(optArea)
    let select = document.createElement("select")
    select.className = "input-group-prepend form-select opt-select"
    addSelectOption(select, textOptions)
    
    let input = document.createElement("input")
    input.className = "form-control opt-input"
    input.setAttribute("placeholder", "value")
    
    grid.append(select, 0, 4)
    grid.append(input, 0, 8)
}

function getCompareTextString(text, optArea) {
    let condict = optArea.find(".opt-select").val()
    let value = optArea.find(".opt-input").val()
    if (value == "") return ""
    return text + condict + value
}

