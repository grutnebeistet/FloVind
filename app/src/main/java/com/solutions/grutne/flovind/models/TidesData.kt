package com.solutions.grutne.flovind.models

import java.util.ArrayList

class TidesData(var stationName: String,
                var stationCode: String,
                var latitude: String,
                var longitude: String,
                var dataType: String,
                var waterlevels: ArrayList<Waterlevel>,
                var errorResponse: String) {

    class Waterlevel(var waterValue: String, var dateTime: String, var flag: String)
}
