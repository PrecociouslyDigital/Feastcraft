# Helpers for harness scenarios, sourced by each one. A scenario runs against a client that
# `scripts/harness up` already started, and stops at the first failed expectation.
set -euo pipefail

harness=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)/scripts/harness

call() {
    "$harness" call "$@"
}

step() {
    echo "== $*"
}

# expect <description> <jq filter> <json>: fails unless the filter holds for the JSON.
expect() {
    local description=$1 filter=$2 json=$3
    if jq -e "$filter" >/dev/null <<<"$json"; then
        echo "ok   $description"
    else
        echo "FAIL $description" >&2
        echo "     expected: $filter" >&2
        jq . <<<"$json" >&2
        exit 1
    fi
}

# run <command>: runs a command as the player and prints the chat lines that came back.
run() {
    call command c="$1"
}

# expect_reply <description> <command> <text>: runs the command and expects a line containing the text.
expect_reply() {
    expect "$1" "any(.lines[]; .text | contains($(jq -n --arg text "$3" '$text')))" "$(run "$2")"
}

# wait_until <description> <jq filter on /status> <max ticks>: checks once a second of game time.
wait_until() {
    local description=$1 filter=$2 max_ticks=$3 status waited=0
    status=$(call status)
    until jq -e "$filter" >/dev/null <<<"$status"; do
        if ((waited >= max_ticks)); then
            expect "$description" "$filter" "$status"
        fi
        status=$(call wait ticks=20)
        waited=$((waited + 20))
    done
    echo "ok   $description"
}

# hotbar_slot <item id>: the first hotbar slot holding the item.
hotbar_slot() {
    call status | jq -e --arg item "$1" 'first(.hotbar[] | select(.item == $item) | .slot)'
}

# shot <name> [hud=false]: takes a screenshot and prints where it went.
shot() {
    local name=$1
    shift
    echo "shot $(call screenshot name="$name" "$@" | jq -r .path)"
}

# respawn: on the death screen, waits for Respawn to enable, presses it, and waits to be back.
respawn() {
    local tries=0
    until call click label=Respawn >/dev/null 2>&1; do
        ((++tries < 20)) || expect "the Respawn button enables" false "$(call status)"
        call wait ticks=5 >/dev/null
    done
    wait_until "the player respawns" '.screen == null and .alive' 200
}
