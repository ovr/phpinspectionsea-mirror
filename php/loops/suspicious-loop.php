<?php

$num_names = 5;
$names = array('', '', '', '', '');
for ($i = 0; $i < $num_names; ++$i) {
    if ($names[$i] === null) {
        /** shall warn here */
        for ($i = 0; $i < $num_names; ++$i) {
            unset($names[$i]);
        }
        $num_names = 0;

        /** shall warn here */
        foreach ($names as $i => $v) {
            unset($names[$i]);
        }

    }
}

foreach ($names as $i => $vv) {
    if ($vv === null) {
        /** shall warn here */
        for ($i = 0, $vv = ''; $i < $num_names; ++$i) {
            unset($names[$i]);
        }
        $num_names = 0;

        /** shall warn here */
        foreach ($names as $i => $v) {
            unset($names[$i]);
        }

    }
}

for ($i = 0; $i > 0, $i < 255; ++$i) {
    echo $i;
}