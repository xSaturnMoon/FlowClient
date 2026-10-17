using System;
using System.IO;
using System.Linq;
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
                var (scale, trans) = EnsureTransformGroup(root, new Point(0.5, 0.5));
                scale.ScaleX = 0.97;
                scale.ScaleY = 0.97;
                trans.X = 0;
                trans.Y = 0;

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

        /// <summary>
        /// Fast, fluid window close transition mimicking native modern Windows behavior.
        /// </summary>
        public static void PlayWindowCloseAnimation(Window window, Action onCompleted)
        {
            if (!AnimationsEnabled)
            {
                onCompleted();
                return;
            }

            var ease = new CubicEase { EasingMode = EasingMode.EaseIn };
            const double dur = 160;

            if (window.Content is FrameworkElement root)
            {
                var (scale, trans) = EnsureTransformGroup(root, new Point(0.5, 0.5));
                var sx = new DoubleAnimation(1.0, 0.95, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var sy = new DoubleAnimation(1.0, 0.95, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var ty = new DoubleAnimation(0, 12, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };

                scale.BeginAnimation(ScaleTransform.ScaleXProperty, sx);
                scale.BeginAnimation(ScaleTransform.ScaleYProperty, sy);
                trans.BeginAnimation(TranslateTransform.YProperty, ty);
            }

            var fade = new DoubleAnimation(window.Opacity, 0, TimeSpan.FromMilliseconds(dur))
            {
                EasingFunction = ease
            };
            fade.Completed += (_, _) => onCompleted();
            window.BeginAnimation(UIElement.OpacityProperty, fade);
        }

        /// <summary>
        /// Smooth minimize transition scaling towards the bottom taskbar with fade.
        /// </summary>
        public static void PlayWindowMinimizeAnimation(Window window, Action onCompleted)
        {
            if (!AnimationsEnabled)
            {
                onCompleted();
                return;
            }

            var ease = new CubicEase { EasingMode = EasingMode.EaseIn };
            const double dur = 150;

            if (window.Content is FrameworkElement root)
            {
                var (scale, trans) = EnsureTransformGroup(root, new Point(0.5, 0.85));
                var sx = new DoubleAnimation(1.0, 0.88, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var sy = new DoubleAnimation(1.0, 0.88, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var ty = new DoubleAnimation(0, 32, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };

                scale.BeginAnimation(ScaleTransform.ScaleXProperty, sx);
                scale.BeginAnimation(ScaleTransform.ScaleYProperty, sy);
                trans.BeginAnimation(TranslateTransform.YProperty, ty);
            }

            var fade = new DoubleAnimation(window.Opacity, 0, TimeSpan.FromMilliseconds(dur))
            {
                EasingFunction = ease
            };
            fade.Completed += (_, _) =>
            {
                onCompleted();
                // Reset transforms and restore opacity ready for restore
                window.Opacity = 1;
                if (window.Content is FrameworkElement r && r.RenderTransform is TransformGroup g)
                {
                    var s = g.Children.OfType<ScaleTransform>().FirstOrDefault();
                    if (s != null) { s.ScaleX = 1; s.ScaleY = 1; }
                    var t = g.Children.OfType<TranslateTransform>().FirstOrDefault();
                    if (t != null) { t.X = 0; t.Y = 0; }
                }
            };
            window.BeginAnimation(UIElement.OpacityProperty, fade);
        }

        /// <summary>
        /// Smooth restore transition expanding smoothly out of the taskbar with fade-in.
        /// </summary>
        public static void PlayWindowRestoreAnimation(Window window)
        {
            if (!AnimationsEnabled)
            {
                window.Opacity = 1;
                return;
            }

            var ease = new CubicEase { EasingMode = EasingMode.EaseOut };
            const double dur = 190;

            window.Opacity = 0;
            if (window.Content is FrameworkElement root)
            {
                var (scale, trans) = EnsureTransformGroup(root, new Point(0.5, 0.85));
                scale.ScaleX = 0.90;
                scale.ScaleY = 0.90;
                trans.Y = 24;

                var sx = new DoubleAnimation(0.90, 1.0, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var sy = new DoubleAnimation(0.90, 1.0, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };
                var ty = new DoubleAnimation(24, 0, TimeSpan.FromMilliseconds(dur)) { EasingFunction = ease };

                scale.BeginAnimation(ScaleTransform.ScaleXProperty, sx);
                scale.BeginAnimation(ScaleTransform.ScaleYProperty, sy);
                trans.BeginAnimation(TranslateTransform.YProperty, ty);
            }

            var fade = new DoubleAnimation(0, 1.0, TimeSpan.FromMilliseconds(dur))
            {
                EasingFunction = ease
            };
            window.BeginAnimation(UIElement.OpacityProperty, fade);
        }

        private static (ScaleTransform scale, TranslateTransform trans) EnsureTransformGroup(FrameworkElement element, Point origin)
        {
            element.RenderTransformOrigin = origin;
            if (element.RenderTransform is TransformGroup group)
            {
                var s = group.Children.OfType<ScaleTransform>().FirstOrDefault();
                if (s == null)
                {
                    s = new ScaleTransform(1, 1);
                    group.Children.Add(s);
                }
                var t = group.Children.OfType<TranslateTransform>().FirstOrDefault();
                if (t == null)
                {
                    t = new TranslateTransform(0, 0);
                    group.Children.Add(t);
                }
                return (s, t);
            }

            var newGroup = new TransformGroup();
            var newScale = new ScaleTransform(1, 1);
            var newTrans = new TranslateTransform(0, 0);
            newGroup.Children.Add(newScale);
            newGroup.Children.Add(newTrans);
            element.RenderTransform = newGroup;
            return (newScale, newTrans);
        }
    }
}
