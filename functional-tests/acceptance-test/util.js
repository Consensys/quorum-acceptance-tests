module.exports = {


    sleep: function (ms) {
        return new Promise(resolve => setTimeout(resolve, ms))
    },

    getRandomInt: function (max) {
        var x = Math.floor(Math.random() * Math.floor(max))
        while (x == 0) {
            x = Math.floor(Math.random() * Math.floor(max))
        }
        return x
    }


}