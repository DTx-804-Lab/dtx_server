// check val is the number
function isNum(val) {
    return !isNaN(val);
}

// pack the DOM element with col class div
function createColDiv(component, size) {
    let result = document.createElement("div");
    result.className = `col-${size}`;
    result.append(component);
    return result;
}

// add select box options
function addSelectOption(select, options) {
    for (const key in options) {
        if (Object.hasOwnProperty.call(options, key)) {
            let option = document.createElement("option");
            option.innerHTML = key;
            option.setAttribute("value", options[key])
            select.append(option);
        }
    }
}

// create bootstrap grid easily
class GridDiv {

    constructor(container) {
        this.container = container
    }

    // append component to specified position
    append(component, row, size) {
        let rows = this.container.getElementsByClassName("row")
        if (row < rows.length) {
            rows[row].append(createColDiv(component, size))
        }
        else if (row == rows.length) {
            let rowDiv = document.createElement("div")
            rowDiv.className = "row"
            rowDiv.append(createColDiv(component, size))
            this.container.append(rowDiv)
        }
    }

}