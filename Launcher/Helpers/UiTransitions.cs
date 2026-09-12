using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Animation;
using Launcher.Services;

namespace Launcher.Helpers
{
    public static class UiTransitions
    {
        private const double DurationMs = 180;
        private const double SlidePx = 8;

        private static bool AnimationsEnabled =>
            LauncherSettingsService.Instance.Current.UiAnimations;

        public static void PlayPageTransition(FrameworkElement target)
        {
            if (!AnimationsEnabled)
            {
                target.Opacity = 1;
                return;
            }
            target.RenderTransform ??= new TranslateTransform();
            if (target.RenderTransform is not TranslateTransform transform)
            {
                transform = new TranslateTransform();
                target.RenderTransform = transform;
            }

            target.RenderTransformOrigin = new Point(0.5, 0.5);

            var opacityOut = new DoubleAnimation(1, 0.88, TimeSpan.FromMilliseconds(DurationMs * 0.45))
            {
                EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
            };
            var slideOut = new DoubleAnimation(0, SlidePx, TimeSpan.FromMilliseconds(DurationMs * 0.45))
            {
                EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
            };

            opacityOut.Completed += (_, _) =>
            {
                var opacityIn = new DoubleAnimation(0.88, 1, TimeSpan.FromMilliseconds(DurationMs * 0.55))
                {
                    EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
                };
                var slideIn = new DoubleAnimation(SlidePx, 0, TimeSpan.FromMilliseconds(DurationMs * 0.55))
                {
                    EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
                };

                target.BeginAnimation(UIElement.OpacityProperty, opacityIn);
                transform.BeginAnimation(TranslateTransform.YProperty, slideIn);
            };

            target.BeginAnimation(UIElement.OpacityProperty, opacityOut);
            transform.BeginAnimation(TranslateTransform.YProperty, slideOut);
        }

        public static void AnimateIndicatorTo(TranslateTransform transform, double targetY, Action? onCompleted = null)
        {
            if (!AnimationsEnabled)
            {
                transform.Y = targetY;
                onCompleted?.Invoke();
                return;
            }
            var animation = new DoubleAnimation(transform.Y, targetY, TimeSpan.FromMilliseconds(220))
            {
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };

            if (onCompleted != null)
                animation.Completed += (_, _) => onCompleted();

            transform.BeginAnimation(TranslateTransform.YProperty, animation);
        }

        public static Task AnimateTranslateYAsync(TranslateTransform transform, double toY, double durationMs = 340)
        {
            if (!AnimationsEnabled)
            {
                transform.Y = toY;
                return Task.CompletedTask;
            }
            var tcs = new TaskCompletionSource<bool>();
            var animation = new DoubleAnimation(transform.Y, toY, TimeSpan.FromMilliseconds(durationMs))
            {
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };

            animation.Completed += (_, _) => tcs.TrySetResult(true);
            transform.BeginAnimation(TranslateTransform.YProperty, animation);
            return tcs.Task;
        }

        public static Task AnimateRotationAsync(RotateTransform transform, double toAngle, double durationMs = 200)
        {
            if (!AnimationsEnabled)
            {
                transform.Angle = toAngle;
                return Task.CompletedTask;
            }
            var tcs = new TaskCompletionSource<bool>();
            var animation = new DoubleAnimation(transform.Angle, toAngle, TimeSpan.FromMilliseconds(durationMs))
            {
                EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseInOut }
            };

            animation.Completed += (_, _) => tcs.TrySetResult(true);
            transform.BeginAnimation(RotateTransform.AngleProperty, animation);
            return tcs.Task;
        }

        public static void AnimateOpacity(FrameworkElement target, double to, double durationMs = 180)
        {
            if (!AnimationsEnabled)
            {
                target.Opacity = to;
                return;
            }
            var animation = new DoubleAnimation(to, TimeSpan.FromMilliseconds(durationMs))
            {
                EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
            };
            target.BeginAnimation(UIElement.OpacityProperty, animation);
        }

        /// <summary>
        /// Plays a premium entrance animation when the launcher opens.
        /// All animations converge to the exact current layout — nothing moves in the final state.
        /// </summary>
        public static void PlayStartupAnimation(
            Window window,
            FrameworkElement? sidebar,
            FrameworkElement? miniSidebar,
            FrameworkElement content)
        {
            // Always fade in the window even if animations are disabled (avoids flash)
            window.Opacity = 0;

            if (!AnimationsEnabled)
            {
                window.Opacity = 1;
                return;
            }

            var ease = new CubicEase { EasingMode = EasingMode.EaseOut };

            // ── 1. Window: fade in ──────────────────────────────────────────
            window.BeginAnimation(UIElement.OpacityProperty,
                new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(480)) { EasingFunction = ease });

            // ── 2. Root content: subtle scale up (0.97 → 1) ────────────────
            if (window.Content is FrameworkElement root)
            {
                var scale = new ScaleTransform(0.97, 0.97);
                root.RenderTransformOrigin = new Point(0.5, 0.5);
                root.RenderTransform = scale;

                var sx = new DoubleAnimation(0.97, 1.0, TimeSpan.FromMilliseconds(500)) { EasingFunction = ease };
                var sy = new DoubleAnimation(0.97, 1.0, TimeSpan.FromMilliseconds(500)) { EasingFunction = ease };
                scale.BeginAnimation(ScaleTransform.ScaleXProperty, sx);
                scale.BeginAnimation(ScaleTransform.ScaleYProperty, sy);
            }

            // ── 3. Sidebar slide in ──
            if (sidebar != null)
            {
                var sidebarSlideIn = new DoubleAnimation(-22, 0, TimeSpan.FromMilliseconds(500))
                {
                    EasingFunction = ease,
                    BeginTime = TimeSpan.FromMilliseconds(30)
                };

                var sidebarT = new TranslateTransform(-22, 0);
                sidebar.RenderTransform = sidebarT;
                sidebarT.BeginAnimation(TranslateTransform.XProperty, sidebarSlideIn);
            }

            if (miniSidebar != null)
            {
                var miniT = new TranslateTransform(-22, 0);
                miniSidebar.RenderTransform = miniT;
                miniT.BeginAnimation(TranslateTransform.XProperty, new DoubleAnimation(-22, 0, TimeSpan.FromMilliseconds(500))
                {
                    EasingFunction = ease,
                    BeginTime = TimeSpan.FromMilliseconds(30)
                });
            }

            // ── 4. Content: slide from right + fade in ──────────────────────
            var contentT = new TranslateTransform(28, 0);
            content.RenderTransform = contentT;
            content.Opacity = 0;

            contentT.BeginAnimation(TranslateTransform.XProperty,
                new DoubleAnimation(28, 0, TimeSpan.FromMilliseconds(520))
                {
                    EasingFunction = ease,
                    BeginTime = TimeSpan.FromMilliseconds(100)
                });
            content.BeginAnimation(UIElement.OpacityProperty,
                new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(460))
                {
                    EasingFunction = ease,
                    BeginTime = TimeSpan.FromMilliseconds(100)
                });
        }

        public static void PlayExitAnimation(
            Window window,
            FrameworkElement? sidebar,
            FrameworkElement? miniSidebar,
            FrameworkElement content,
            Action onCompleted)
        {
            if (!AnimationsEnabled)
            {
                onCompleted();
                return;
            }

            var ease = new CubicEase { EasingMode = EasingMode.EaseIn };
            const double dur = 320;

            // Content: slide right + fade out
            var contentT = content.RenderTransform as TranslateTransform ?? new TranslateTransform();
            content.RenderTransform = contentT;
            contentT.BeginAnimation(TranslateTransform.XProperty,
                new DoubleAnimation(0, 24, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease });
            content.BeginAnimation(UIElement.OpacityProperty,
                new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(dur * 0.85)) { EasingFunction = ease });

            // Sidebar: move out
            var sidebarSlideOut = new DoubleAnimation(0, -22, TimeSpan.FromMilliseconds(dur))
            {
                EasingFunction = ease,
                BeginTime = TimeSpan.FromMilliseconds(30)
            };

            if (sidebar != null)
            {
                var sidebarT = sidebar.RenderTransform as TranslateTransform ?? new TranslateTransform();
                sidebar.RenderTransform = sidebarT;
                sidebarT.BeginAnimation(TranslateTransform.XProperty, sidebarSlideOut);
            }

            if (miniSidebar != null)
            {
                var miniT = miniSidebar.RenderTransform as TranslateTransform ?? new TranslateTransform();
                miniSidebar.RenderTransform = miniT;
                miniT.BeginAnimation(TranslateTransform.XProperty, sidebarSlideOut);
            }

            // Window: fade out — fire onCompleted when done
            var windowFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(dur + 60))
            {
                EasingFunction = ease,
                BeginTime = TimeSpan.FromMilliseconds(30)
            };
            windowFade.Completed += (_, _) => onCompleted();
            window.BeginAnimation(UIElement.OpacityProperty, windowFade);
        }
    }
}
