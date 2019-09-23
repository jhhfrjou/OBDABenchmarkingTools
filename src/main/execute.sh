obdaconvert ucq "$1" "$3" rapid --string
psql -U postgres -d temp -c "SELECT COUNT(*) FROM (${SQL%?}) AS query;"