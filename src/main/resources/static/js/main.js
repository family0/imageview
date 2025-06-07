"use strict"
function openImg() {
  console.log("aaa")
  const url = event.target.src.replace('/image', '/open');
  fetch(url);
}
