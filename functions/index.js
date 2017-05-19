'use strict';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

const MIN_DIST = 0.001;
const MAX_DIST = 1000000000000000;
var points;

class Point {
  constructor(lat, lon){
    this.id = null;
    this.lat = lat;
    this.lon = lon;
    this.pictures = null;
  }
}

class Picture {
  constructor(description, id, likes, views, popularity, name, path, timestamp, userIcon, userId, username){
    this.id = id;
    this.description = description;
    this.likes = likes;
    this.views = views;
    this.popularity = popularity;
    this.name = name;
    this.path = path;
    this.timestamp = timestamp;
    this.userIcon = userIcon;
    this.userId = userId;
    this.username = username;
  }
}
var databaseRef = admin.database().ref();
exports.aggregatePlaces = functions.https.onRequest((req, res) => {

  // read from the database
  var ref = databaseRef.child("places");
  ref.once("value", function(snapshot) {
    // console.log("snapshot:" + snapshot);
    var places = snapshot.val();

    var ref = databaseRef.child("points");
    ref.once("value", function(snapshot) {
      points = snapshot.val();

      aggregatePlaces(places, points);
    });
  });
  console.log('Function successfully executed');
  res.status(200).end();
});

function aggregatePlaces(places, points){
  for (var keyPlace in places){
    var place = places[keyPlace];
    var placeLat = parseFloat(place.lat);
    var placeLon  = parseFloat(place.lon);
    var minDist = MAX_DIST;
    var minPointKey = null;

    for (var keyPoint in points){
      var point = points[keyPoint];
      var pointLat = parseFloat(point.lat);
      var pointLon = parseFloat(point.lon);
      if ((placeLat <= pointLat+MIN_DIST) && (placeLat >= pointLat-MIN_DIST) &&
          (placeLon <= pointLon+MIN_DIST) && (placeLon >= pointLon-MIN_DIST)){
            var dist = computeDist(place, point);
            if (dist < minDist){
              minDist = dist;
              minPointKey = keyPoint;
            }
      }
    }
    if (minPointKey === null)
      createPoint(place);
    else
      merge(place, points[minPointKey]);
  }
}

function merge(place, point){
  var pic;
  for (var key in place.picture)
	  pic = place.picture[key];
  var pushRef = databaseRef.child("points").child(point.id).child("pictures").child(pic.id);
  if (pic != null){
	pushRef.set(pic);
  }

  deletePlace(place.id);
}

function createPoint(place){
  var toPut = new Point(place.lat, place.lon);
  var pointsRef = databaseRef.child("points");
  var pushRef = pointsRef.push();
  toPut.id = pushRef.key;
  toPut.pictures = place.picture;
  points[place.id] = toPut;
  pushRef.set(toPut);

  deletePlace(place.id);
}

function computeDist(place, point){
  var placeLat = place.lat;
  var placeLon  = place.lon;
  var pointLat = point.lat;
  var pointLon = point.lon;
  var diffLat = placeLat-pointLat;
  var diffLon = placeLon-pointLon;
  return diffLat*diffLat + diffLon*diffLon;
}

function hashCode(str){
	var hash = 0;
	if (str.length == 0) return hash;
	for (i = 0; i < str.length; i++) {
		char = str.charCodeAt(i);
    hash = ((hash<<5)-hash)+char;
    hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
}

function deletePlace(placeId){
  var placesRef = databaseRef.child("places");
  placesRef.child(placeId).set(null);
}
