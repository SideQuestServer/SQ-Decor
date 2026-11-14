package com.sqdecor.led;

public interface ChannelMember {
    int getChannel();

    void channelChanged(boolean on);
}
