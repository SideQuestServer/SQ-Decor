package com.sqdecor.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("9");
        registrar.playToServer(LedTunerPayload.TYPE, LedTunerPayload.STREAM_CODEC, LedTunerPayload::handle);
        registrar.playToServer(LampPaintPayload.TYPE, LampPaintPayload.STREAM_CODEC, LampPaintPayload::handle);
        registrar.playToServer(LedBrightnessPayload.TYPE, LedBrightnessPayload.STREAM_CODEC, LedBrightnessPayload::handle);
        registrar.playToServer(LedCopyPayload.TYPE, LedCopyPayload.STREAM_CODEC, LedCopyPayload::handle);
        registrar.playToServer(MarqueePayload.TYPE, MarqueePayload.STREAM_CODEC, MarqueePayload::handle);
        registrar.playToServer(DeviceSettingsPayload.TYPE, DeviceSettingsPayload.STREAM_CODEC, DeviceSettingsPayload::handle);
        registrar.playToServer(RemotePayload.TYPE, RemotePayload.STREAM_CODEC, RemotePayload::handle);
        registrar.playToClient(RemoteStatusPayload.TYPE, RemoteStatusPayload.STREAM_CODEC, RemoteStatusPayload::handle);
        registrar.playToClient(ImageMonitorPayload.TYPE, ImageMonitorPayload.STREAM_CODEC, ImageMonitorPayload::handle);
    }
}
