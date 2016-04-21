//
//  SOSPicker.m
//  SyncOnSet
//
//  Created by Christopher Sullivan on 10/25/13.
//
//

#import "SOSPicker.h"
#import "ELCAlbumPickerController.h"
#import "ELCImagePickerController.h"
#import "ELCAssetTablePicker.h"

#define CDV_PHOTO_PREFIX @"cdv_photo_"

@implementation SOSPicker

@synthesize callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command {
	NSDictionary *options = [command.arguments objectAtIndex: 0];

	NSInteger maximumImagesCount = [[options objectForKey:@"maximumImagesCount"] integerValue];
	self.width = [[options objectForKey:@"width"] integerValue];
	self.height = [[options objectForKey:@"height"] integerValue];
	self.quality = [[options objectForKey:@"quality"] integerValue];

	// Create the an album controller and image picker
	ELCAlbumPickerController *albumController = [[ELCAlbumPickerController alloc] init];

	if (maximumImagesCount == 1) {
      albumController.immediateReturn = true;
      albumController.singleSelection = true;
   } else {
      albumController.immediateReturn = false;
      albumController.singleSelection = false;
   }

   ELCImagePickerController *imagePicker = [[ELCImagePickerController alloc] initWithRootViewController:albumController];
   imagePicker.maximumImagesCount = maximumImagesCount;
    self.returnsOriginalImage = YES;
   imagePicker.returnsOriginalImage = self.returnsOriginalImage;
   imagePicker.imagePickerDelegate = self;

   albumController.parent = imagePicker;
	self.callbackId = command.callbackId;
	// Present modally
	[self.viewController presentViewController:imagePicker
	                       animated:YES
	                     completion:nil];
}

-(void)handleInfoObjects:(NSArray*)info
{
    CDVPluginResult* result = nil;
    NSMutableArray *resultStrings = [[NSMutableArray alloc] init];
    NSData* data = nil;
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
    NSError* err = nil;
    NSFileManager* fileMgr = [[NSFileManager alloc] init];
    NSString* filePath;
    ALAsset* asset = nil;
    UIImageOrientation orientation = UIImageOrientationUp;;
    CGSize targetSize = CGSizeMake(self.width, self.height);
    int counter = 0;
    for (NSDictionary *dict in info) {
        asset = [dict objectForKey:@"ALAsset"];
        // From ELCImagePickerController.m
        
        int i = 1;
        do {
            filePath = [NSString stringWithFormat:@"%@/%@%03d.%@", docsPath, CDV_PHOTO_PREFIX, i++, @"jpg"];
        } while ([fileMgr fileExistsAtPath:filePath]);
        
        @autoreleasepool {
            ALAssetRepresentation *assetRep = [asset defaultRepresentation];
            CGImageRef imgRef = NULL;
            
            //defaultRepresentation returns image as it appears in photo picker, rotated and sized,
            //so use UIImageOrientationUp when creating our image below.
            if (self.returnsOriginalImage) {
                imgRef = [assetRep fullResolutionImage];
                orientation = [assetRep orientation];
            } else {
                imgRef = [assetRep fullScreenImage];
            }
            
            UIImage* image = [UIImage imageWithCGImage:imgRef scale:1.0f orientation:orientation];
            if (self.width == 0 && self.height == 0) {
                data = UIImageJPEGRepresentation(image, self.quality/100.0f);
            } else {
                UIImage* scaledImage = [self imageByScalingNotCroppingForSize:image toSize:targetSize];
                data = UIImageJPEGRepresentation(scaledImage, self.quality/100.0f);
            }
            
            if (![data writeToFile:filePath options:NSAtomicWrite error:&err]) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                break;
            }
            else
            {
                NSMutableDictionary * json = [NSMutableDictionary dictionary];
                [json setObject:[[NSURL fileURLWithPath:filePath] absoluteString] forKey:@"fileName"];
                [json setObject:[NSString stringWithFormat:@"%d", counter++] forKey:@"position"];
                NSData *jsonData = [NSJSONSerialization dataWithJSONObject:json options:0 error:&err];
                if(!jsonData)
                {
                    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                    break;
                }
                [resultStrings addObject:[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]];
                
                //[resultStrings addObject:[[NSURL fileURLWithPath:filePath] absoluteString]];
            }
        }
        
    }
    
    if (nil == result) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:resultStrings];
    }
    [self performSelectorOnMainThread:@selector(doneWithResult:) withObject:result waitUntilDone:NO];
}

-(void)doneWithResult:(CDVPluginResult*)result
{
    [self.alertView dismissWithClickedButtonIndex:0 animated:YES];
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}


- (void)elcImagePickerController:(ELCImagePickerController *)picker didFinishPickingMediaWithInfo:(NSArray *)info {
    
    self.alertView = [self alertViewWithActivityIndicator:@"Behandler" delegate:self];
    [self.alertView show];
    
    
    /*[[UIAlertView alloc] initWithTitle:@"Behandler" message:nil delegate:self cancelButtonTitle:nil otherButtonTitles:nil, nil];
    
    UIActivityIndicatorView *indicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    [indicator startAnimating];
    indicator.center = self.viewController.view.center;
    
    [self.alertView setValue:indicator forKey:@"accessoryView"];
    
    [self.alertView show];*/
    
    
    
    [self performSelectorInBackground:@selector(handleInfoObjects:) withObject:info];
}

- (UIAlertView *)alertViewWithActivityIndicator:(NSString *)title delegate:(id<UIAlertViewDelegate>)delegate;
{
    UIAlertView *a = [[UIAlertView alloc] initWithTitle:title
                                                message:nil
                                               delegate:delegate
                                      cancelButtonTitle:nil
                                      otherButtonTitles:nil];
    
    UIView *v = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 50, 50)];
    UIActivityIndicatorView *aiv = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    aiv.translatesAutoresizingMaskIntoConstraints = NO;
    [v addSubview:aiv];
    [v addConstraint:[NSLayoutConstraint constraintWithItem:aiv
                                                  attribute:NSLayoutAttributeCenterX
                                                  relatedBy:NSLayoutRelationEqual
                                                     toItem:v
                                                  attribute:NSLayoutAttributeCenterX
                                                 multiplier:1.0
                                                   constant:0]];
    [v addConstraint:[NSLayoutConstraint constraintWithItem:aiv
                                                  attribute:NSLayoutAttributeCenterY
                                                  relatedBy:NSLayoutRelationEqual
                                                     toItem:v
                                                  attribute:NSLayoutAttributeCenterY
                                                 multiplier:1.0
                                                   constant:0]];
    [aiv startAnimating];
    [a setValue:v forKey:@"accessoryView"];
    
    return a;
}

- (void)elcImagePickerControllerDidCancel:(ELCImagePickerController *)picker {
	[self.viewController dismissViewControllerAnimated:YES completion:nil];
	CDVPluginResult* pluginResult = nil;
    NSArray* emptyArray = [NSArray array];
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:emptyArray];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
}

- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize
{
    UIImage* sourceImage = anImage;
    UIImage* newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = frameSize.width;
    CGFloat targetHeight = frameSize.height;
    CGFloat scaleFactor = 0.0;
    CGSize scaledSize = frameSize;

    if (CGSizeEqualToSize(imageSize, frameSize) == NO) {
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;

        // opposite comparison to imageByScalingAndCroppingForSize in order to contain the image within the given bounds
        if (widthFactor == 0.0) {
            scaleFactor = heightFactor;
        } else if (heightFactor == 0.0) {
            scaleFactor = widthFactor;
        } else if (widthFactor > heightFactor) {
            scaleFactor = heightFactor; // scale to fit height
        } else {
            scaleFactor = widthFactor; // scale to fit width
        }
        scaledSize = CGSizeMake(width * scaleFactor, height * scaleFactor);
    }

    UIGraphicsBeginImageContext(scaledSize); // this will resize

    [sourceImage drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];

    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }

    // pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}

@end
