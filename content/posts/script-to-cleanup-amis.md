---
author: "Andy Rea"
title: "Small script to clean up your AMIs"
date: 2020-11-10
draft: false
---

On a little side project I am working on I am creating my own AMIs using Packer and AWS Launch Templates.  Whilst developing the AMI you start to get a build up of previous versions along with their associated snapshot.  I wanted to have some script which I could run which would remove each AMI and related snapshot prior to me building the next iteration.

Using the AWS CLI you need to execute the following two CLI commands to completely clear up:

- aws ec2 deregister-image
- aws ec2 delete-snapshot

Another feature I wanted was to allow me to filter the AMI set based on the name of the AMI.  I have a couple of projects so I just wanted to focus on the AMIs relevant to the project.  For this and the driver behind the entire script I used `jq`.  The steps are:

1.  Fetch all the AMIs which I own.
2.  Create an argument for jq to filter on name and pass in the first argument passed to the bash script.
3.  Select all images whose name contains the name filter argument.
4.  Sort the results by CreationDate and reverse the items so we have the most recent first.  
5.  Select all items apart from the first
6.  Create a projection of the ImageId, SnapshotId and Name
7.  Output TSV
8.  Read each line, defining the separator and tab
9.  Delete the Image
10. Delete the Snahot 

I have also put `export AWS_PAGER=""` to avoid the need for key presses to page through the results of each AWS command.

Couple of example usages are:

```
# This will find any AMIs whose name contains 'fubar'.
./clean-up-amis.sh fubar

# This will delete ALL AMIs apart from the most recent since no filter is supplied.
./clean-up-amis.sh 
```

## The script!

```
#!/usr/bin/env bash
export AWS_PAGER=""
aws ec2 describe-images --owners self | \
  jq -r --arg image_name "$1" '[.Images[] | 
                              select(.Name | contains($image_name))] | 
                              sort_by(.CreationDate) | 
                              reverse | .[1:] | .[] | 
                              [.ImageId, (.BlockDeviceMappings[] | select(.DeviceName == "/dev/sda1").Ebs.SnapshotId), .Name] | 
                              @tsv' | \
  while IFS=$'\t' read -r image snapshot name; do
    echo "Cleaning up AMI $name"
    printf "De-registering image $image.."
    aws ec2 deregister-image --image-id "$image"
    echo "Done"
    printf "Deleting snapshot $snapshot.."
    aws ec2 delete-snapshot --snapshot-id "$snapshot"
    echo "Done"
    echo
  done
```
