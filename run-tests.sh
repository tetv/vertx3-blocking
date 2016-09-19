#!/bin/sh
if [ $# = 0 ]; then
  echo "Usage: $0 <num> ..."
  echo "Where <num> can be also:"
  echo "  1x # means 11, 12, 13 (Direct code)"
  echo "  2x # means 21, 22, 23, 24, 25 and 26 (vertx.executeBlocking - ordered and not ordered)"
  echo "  3x # means 31, 32, 33, 34, 35 and 36 (executor.executeBlocking - ordered and not ordered)"
  echo "  4x # means 41, 42, 43, 44, 45 and 46 (java thread - ordered and not ordered)"
  echo "  xx # maans 1x, 2x, 3x and 4x"
  exit 1
fi

# CONFIGURATION
PONG_WAIT=-Dwait=50
PING_TIMES=-Dtimes=500
#ASYNC="-Dvertx.hazelcast.async-api=true"

# DON'T NEED TO CHANGE
TAB="                             "
JAVA_OPT="-Xss2m -Xms64m -Xmx1g -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 $ASYNC"
OPTS="--java-opts=$JAVA_OPT"
NUM_LINES=$(expr 4 "*" ${PING_TIMES##*=})

report() {
  START=$(date +%s%N)
  OUT=test$1.log
  shift
  
  echo "Running [$OUT]: Waiting for $NUM_LINES entries"
  sleep 5
  while [ $(cat java*.log | fgrep "ball" | wc -l) != $NUM_LINES ]; do
    sleep 0.5
  done
  echo "Running [$OUT]: Finihed with duration: " $(expr $(date +%s%N) - $START | sed -E -e "s/([0-9]{3})[0-9]{6}$/.\1s/")

  echo "Stoping [$OUT]: Waiting for all verticles to stop"
  for f in $@; do vertx stop $f | fgrep -v Stopping; done
  while ! vertx list | grep -q "No vert"; do sleep 0.2; done
  echo "Stoping [$OUT]: Finished with duration: " $(expr $(date +%s%N) - $START | sed -E -e "s/([0-9]{3})[0-9]{6}$/.\1s/")

  case $# in
    1) cat java* | fgrep ">" | sort | sed -E -e "s/(vert.x-)?([a-z]).*-thread-/\2-0/" -e "s/-0*([0-9][0-9]\])/-\1/" > $OUT;;
    2) cat java* | fgrep ">" | sort | sed -E -e "s/(vert.x-)?([a-z]).*-thread-/\2-0/" -e "s/-0*([0-9][0-9]\])/-\1/" \
           -e "s/ \[.*$2>/$TAB&/" > $OUT;;
    3) cat java* | fgrep ">" | sort | sed -E -e "s/(vert.x-)?([a-z]).*-thread-/\2-0/" -e "s/-0*([0-9][0-9]\])/-\1/" \
           -e "s/ \[.*$2>/$TAB&/" -e "s/ \[.*$3>/$TAB$TAB&/" > $OUT;;
    4) cat java* | fgrep ">" | sort | sed -E -e "s/(vert.x-)?([a-z]).*-thread-/\2-0/" -e "s/-0*([0-9][0-9]\])/-\1/" \
           -e "s/ \[.*$2>/$TAB&/" -e "s/ \[.*$3>/$TAB$TAB&/" -e "s/ \[.*$4>/$TAB$TAB$TAB&/" > $OUT;;
    5) cat java* | fgrep ">" | sort | sed -E -e "s/(vert.x-)?([a-z]).*-thread-/\2-0/" -e "s/-0*([0-9][0-9]\])/-\1/" \
           -e "s/ \[.*$2>/$TAB&/" -e "s/ \[.*$3>/$TAB$TAB&/" -e "s/ \[.*$4>/$TAB$TAB$TAB&/"-e "s/ \[.*$5>/$TAB$TAB$TAB$TAB&/" > $OUT;;
  esac
  mv java0.log "${OUT%%.*}_pong.log"
  mv java1.log "${OUT%%.*}_ping.log"
}

rm -f java*
for n in $@; do
  case $n in
    1x) $0 11 12 13;;
    2x) $0 21 22 23 24 25 26;;
    3x) $0 31 32 33 34 35 36;;
    4x) $0 41 42 43 44 45 46;;
    xx) $0 1x 2x 3x 4x;;

    11)
       echo java $JAVA_OPT -cp "'target/*'" io.vertx.core.Launcher start com.tetv.verticles.Pong -id pong  "'$OPTS'" $PONG_WAIT -Dmode=default               -cluster
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=default               -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    12)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=default               -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    13)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=default               -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;

    21)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=block -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    22)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=block -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    23)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=block -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    24)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=block -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    25)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=block -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    26)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=block -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;

    31)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    32)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    33)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    34)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    35)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    36)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=wexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;

    41)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    42)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    43)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=true  -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    44)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong         "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    45)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Pong   -id pong -worker "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;
    46)
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.PongMT -id pong -worker "$OPTS" $PONG_WAIT -Dmode=jexec -Dordered=false -cluster
       sleep 1
       java $JAVA_OPT -cp 'target/*' io.vertx.core.Launcher start com.tetv.verticles.Ping   -id ping -worker "$OPTS" $PING_TIMES                             -cluster
       report $n ping pong
    ;;

    *) echo "Error: Option $n not supported"; exit 2;;
  esac
done
