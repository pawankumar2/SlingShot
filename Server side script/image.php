<?php
  $target = 'uploads/';
  if (!file_exists($target)) {
    mkdir($target, 0777, true);
}
  if(move_uploaded_file( $_FILES['image']['tmp_name'], $target.$_FILES['image']['name']))
  	echo "Success";
 else 
 	echo "Failed";
?>