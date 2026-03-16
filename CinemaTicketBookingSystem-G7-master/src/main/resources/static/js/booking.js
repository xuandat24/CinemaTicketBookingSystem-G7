let seatPrice = 100000

let comboPrice = {
    1:48000,
    2:88000,
    3:220000,
    4:68000,
    5:130000
}

let comboQty = {
    1:0,
    2:0,
    3:0,
    4:0,
    5:0
}

function changeCombo(id,change){

    comboQty[id]+=change

    if(comboQty[id]<0){
        comboQty[id]=0
    }

    document.getElementById("combo"+id).innerText=comboQty[id]

    updateTotal()

}

function updateTotal(){

    let comboTotal=0

    for(let id in comboQty){
        comboTotal+=comboQty[id]*comboPrice[id]
    }

    let discount = document.getElementById("discount").innerText

    let final = seatPrice + comboTotal - discount

    document.getElementById("totalPrice").innerText = seatPrice + comboTotal

    document.getElementById("finalPrice").innerText = final

}

document.getElementById("pointInput").addEventListener("input",function(){

    let point = this.value

    let discount = point * 1000

    document.getElementById("discountMoney").innerText = discount
    document.getElementById("discount").innerText = discount

    updateTotal()

})


let time = 600

let timer = setInterval(function(){

    let minutes = Math.floor(time/60)
    let seconds = time%60

    document.getElementById("timer").innerText =
        minutes + ":" + (seconds<10?"0":"") + seconds

    time--

    if(time<0){

        clearInterval(timer)

        alert("Hết thời gian giữ ghế!")

    }

},1000)