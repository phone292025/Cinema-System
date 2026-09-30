"use client";

import { Camera, CameraOff } from "lucide-react";
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";

type DetectedBarcode = { rawValue: string };
type BarcodeDetectorLike = { detect: (source: CanvasImageSource) => Promise<DetectedBarcode[]> };
type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike;

function getDetectorConstructor(): BarcodeDetectorConstructor | null {
  if (typeof window === "undefined") return null;
  return (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor }).BarcodeDetector ?? null;
}

function cameraErrorMessage(error: unknown) {
  if (error instanceof DOMException && error.name === "NotFoundError") {
    return "No camera was found on this device. Enter the code by hand instead.";
  }
  return "Camera access was refused. Allow the camera, or enter the code by hand.";
}

export function QrScanner({ onScan }: { onScan: (value: string) => void }) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const frameRef = useRef<number | null>(null);
  const startingRef = useRef(false);
  const mountedRef = useRef(false);
  const [active, setActive] = useState(false);
  const [error, setError] = useState("");

  const supported = useSyncExternalStore(
    () => () => {},
    () => getDetectorConstructor() !== null && !!navigator.mediaDevices,
    () => null,
  );

  const stop = useCallback(() => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
    frameRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    if (mountedRef.current) setActive(false);
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      stop();
    };
  }, [stop]);

  async function start() {
    if (startingRef.current || streamRef.current) return;
    setError("");
    const Detector = getDetectorConstructor();
    if (!Detector) {
      setError("This browser cannot scan with the camera. Type or scan the code into the field instead.");
      return;
    }

    startingRef.current = true;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
      // The page may have been left while the permission prompt was open.
      if (!mountedRef.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }
      streamRef.current = stream;
      setActive(true);

      const video = videoRef.current;
      if (!video) {
        stop();
        return;
      }
      video.srcObject = stream;
      await video.play();
      if (streamRef.current !== stream) return;

      const detector = new Detector({ formats: ["qr_code"] });
      const tick = async () => {
        if (streamRef.current !== stream) return;
        try {
          const results = await detector.detect(video);
          if (streamRef.current !== stream) return;
          const value = results[0]?.rawValue;
          if (value) {
            stop();
            onScan(value);
            return;
          }
        } catch {
          // The first frames can arrive before the video has data; keep scanning.
        }
        frameRef.current = requestAnimationFrame(() => void tick());
      };
      frameRef.current = requestAnimationFrame(() => void tick());
    } catch (err) {
      if (mountedRef.current) setError(cameraErrorMessage(err));
      stop();
    } finally {
      startingRef.current = false;
    }
  }

  return (
    <div>
      <div className="flex flex-wrap gap-3">
        {active ? (
          <button
            type="button"
            onClick={stop}
            className="flex items-center gap-2 rounded-md border border-line px-4 py-3 font-semibold text-muted hover:border-danger hover:text-danger"
          >
            <CameraOff size={18} aria-hidden />
            Stop camera
          </button>
        ) : (
          <button
            type="button"
            onClick={start}
            disabled={supported === false}
            className="flex items-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Camera size={18} aria-hidden />
            Scan with camera
          </button>
        )}
      </div>

      {supported === false && (
        <p className="mt-3 text-sm text-muted">
          Camera scanning is not available in this browser. A USB barcode scanner still works: focus the field below and scan.
        </p>
      )}
      {error && (
        <p role="alert" className="mt-3 rounded-md border border-danger/40 bg-danger/10 p-3 text-sm text-danger">
          {error}
        </p>
      )}

      <div className={active ? "mt-4 overflow-hidden rounded-md border border-accent/40" : "hidden"}>
        <video ref={videoRef} aria-label="Camera preview for ticket scanning" className="aspect-video w-full bg-black object-cover" playsInline muted />
      </div>
    </div>
  );
}
