#sh

# This command generates the Activity.proto Java classes.
# NOTE: It must be run from the folder containing the script, and the protobuf compiler
# must have already been installed on your system

protoc --java_out=../java ActivityModel.proto
